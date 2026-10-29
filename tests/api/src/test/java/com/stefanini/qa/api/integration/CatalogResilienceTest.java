package com.stefanini.qa.api.integration;

import static com.stefanini.qa.api.data.Products.KEYBOARD;
import static com.stefanini.qa.api.data.Products.MOUSE;
import static com.stefanini.qa.api.data.Products.NOTEBOOK;
import static com.stefanini.qa.api.support.ApiAssertions.assertError;
import static com.stefanini.qa.api.support.ApiAssertions.assertNothingPersisted;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.data.OrderPayload;
import com.stefanini.qa.api.data.TestClient;
import com.stefanini.qa.api.fault.CatalogProxy;
import com.stefanini.qa.api.support.Api;
import com.stefanini.qa.api.support.Orders;
import com.stefanini.qa.api.support.KnownDefect;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * docs/test-cases/integration.md — resiliência e contrato da chamada ao catálogo, com falhas
 * injetadas pelo proxy WireMock (tests/infra/docker-compose.qa.yml). Os stubs de falha são removidos
 * após cada caso; o proxy volta a ser transparente.
 */
@Tag("api")
@Tag("integration")
@Tag("fault")
@Tag("serial")
@DisplayName("Integração — resiliência com o catálogo (injeção de falhas)")
class CatalogResilienceTest {

    private static final String X_REQUEST_ID = "X-Request-Id";

    @BeforeEach
    void cleanProxy() {
        CatalogProxy.reset();
    }

    @AfterEach
    void restoreProxy() {
        CatalogProxy.awaitReady();
        CatalogProxy.reset();
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-INT-004 — uma única chamada em lote ao catálogo por pedido (anti-N+1)")
    void singleBatchCallPerOrder() {
        Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(NOTEBOOK, 1).item(MOUSE, 1).item(KEYBOARD, 1));

        List<Map<String, Object>> received = CatalogProxy.receivedRequests();

        assertThat(received).as("chamadas recebidas pelo catálogo").hasSize(1);
        Map<String, Object> call = received.get(0);
        assertThat(call).containsEntry("method", "POST").containsEntry("url", CatalogProxy.PRICES_PATH);
        assertThat((String) call.get("body")).contains(NOTEBOOK, MOUSE, KEYBOARD);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-INT-005 — X-Request-Id propagado até o catálogo")
    void requestIdIsPropagated() {
        String requestId = "it-" + TestClient.unique().email().substring(3, 16);

        Response response = Api.orders().header(X_REQUEST_ID, requestId)
                .body(OrderPayload.of(TestClient.unique()).item(MOUSE, 1).build()).post();

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.header(X_REQUEST_ID)).isEqualTo(requestId);
        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) CatalogProxy.receivedRequests().get(0).get("headers");
        assertThat(headers).as("headers da chamada ao catálogo").containsEntry(X_REQUEST_ID, requestId);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-INT-006 — catálogo derruba a conexão → 503 CATALOG_UNAVAILABLE e recuperação sem reinício")
    @KnownDefect(value = "5", suspicion = "H-11")
    void catalogUnavailableAndRecovery() {
        TestClient client = TestClient.unique();
        CatalogProxy.failPricesWithConnectionReset();

        Response unavailable = Orders.create(OrderPayload.of(client).item(MOUSE, 1).build());

        assertError(unavailable, 503, "CATALOG_UNAVAILABLE");
        assertNothingPersisted(client.email());

        CatalogProxy.reset();
        Response recovered = Orders.create(OrderPayload.of(TestClient.unique()).item(MOUSE, 1).build());
        assertThat(recovered.statusCode()).as("recuperação após o catálogo voltar").isEqualTo(201);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-INT-007 — catálogo lento além do limite (5 s) → 504 CATALOG_TIMEOUT")
    @KnownDefect(value = "5", suspicion = "H-11")
    void catalogSlowerThanTimeout() {
        TestClient client = TestClient.unique();
        CatalogProxy.delayPrices(5_000);

        long start = System.nanoTime();
        Response response = Orders.create(OrderPayload.of(client).item(MOUSE, 1).build());
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        Allure.addAttachment("Tempo até a resposta", elapsed.toMillis() + " ms (status " + response.statusCode() + ")");
        assertError(response, 504, "CATALOG_TIMEOUT");
        assertNothingPersisted(client.email());
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-INT-008 — catálogo lento dentro do limite (2 s) → pedido criado normalmente")
    void catalogSlowWithinTimeout() {
        CatalogProxy.delayPrices(2_000);

        Response response = Orders.create(OrderPayload.of(TestClient.unique()).item(MOUSE, 1).build());

        assertThat(response.statusCode()).as("corpo: %s", response.asString()).isEqualTo(201);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-INT-009 — catálogo responde erro 5xx → 503 CATALOG_UNAVAILABLE")
    void catalogServerError() {
        TestClient client = TestClient.unique();
        CatalogProxy.respondPricesWithStatus(500);

        Response response = Orders.create(OrderPayload.of(client).item(MOUSE, 1).build());

        assertError(response, 503, "CATALOG_UNAVAILABLE");
        assertNothingPersisted(client.email());
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-INT-010 — catálogo inacessível não afeta a leitura de pedidos")
    void readsDoNotDependOnCatalog() {
        Response created = Orders.createSimple();
        CatalogProxy.failPricesWithConnectionReset();

        Response detail = Orders.get(created.jsonPath().getString("id"));
        Response list = Api.orders().get();

        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(detail.jsonPath().getMap("items[0]")).isEqualTo(created.jsonPath().getMap("items[0]"));
        assertThat(list.statusCode()).isEqualTo(200);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-INT-015 — catálogo inacessível → 503/504 classificado (Q-12) e recuperação")
    void catalogUnreachableAndRecovery() {
        TestClient client = TestClient.unique();
        Response unreachable;
        Duration recovery;
        CatalogProxy.stopContainer();
        try {
            unreachable = Orders.create(OrderPayload.of(client).item(MOUSE, 1).build());
        } finally {
            recovery = CatalogProxy.startContainer();
        }
        Allure.addAttachment("Tempo de recuperação", recovery.toMillis()
                + " ms entre religar o destino e o order-service voltar a consultá-lo (sem reinício)");

        Map<Integer, String> documentedClassifications = Map.of(503, "CATALOG_UNAVAILABLE", 504, "CATALOG_TIMEOUT");
        assertThat(documentedClassifications).as("classificação documentada para destino inacessível — corpo: %s",
                unreachable.asString()).containsKey(unreachable.statusCode());
        assertError(unreachable, unreachable.statusCode(), documentedClassifications.get(unreachable.statusCode()));
        Allure.addAttachment("Q-12 — classificação observada", unreachable.statusCode() + " "
                + documentedClassifications.get(unreachable.statusCode()));
        assertNothingPersisted(client.email());
        Response recovered = Orders.create(OrderPayload.of(TestClient.unique()).item(MOUSE, 1).build());
        assertThat(recovered.statusCode()).as("recuperação após o destino voltar").isEqualTo(201);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-INT-011 — validações de entrada não dependem do catálogo (400, não 503)")
    void inputValidationPrecedesCatalogCall() {
        CatalogProxy.failPricesWithConnectionReset();

        Response duplicated = Orders.create(OrderPayload.of(TestClient.unique()).item(MOUSE, 1).item(MOUSE, 1).build());
        Response zeroQuantity = Orders.create(OrderPayload.of(TestClient.unique()).item(MOUSE, 0).build());
        Response invalidEmail = Orders.create(OrderPayload.of(TestClient.unique()).clientField("email", "maria@")
                .item(MOUSE, 1).build());

        assertError(duplicated, 400, "VALIDATION_ERROR");
        assertError(zeroQuantity, 400, "VALIDATION_ERROR");
        assertError(invalidEmail, 400, "VALIDATION_ERROR");
        assertThat(CatalogProxy.receivedRequests()).as("catálogo não é consultado").isEmpty();
    }
}
