package com.stefanini.qa.api.integration;

import static com.stefanini.qa.api.data.Products.KEYBOARD;
import static com.stefanini.qa.api.data.Products.MOUSE;
import static com.stefanini.qa.api.data.Products.NOTEBOOK;
import static com.stefanini.qa.api.support.ApiAssertions.assertError;
import static com.stefanini.qa.api.support.ApiAssertions.assertNothingPersisted;
import static com.stefanini.qa.api.support.Money.assertAmount;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.data.OrderPayload;
import com.stefanini.qa.api.data.TestClient;
import com.stefanini.qa.api.support.Api;
import com.stefanini.qa.api.support.Ids;
import com.stefanini.qa.api.support.Orders;
import com.stefanini.qa.api.support.KnownDefect;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** docs/test-cases/integration.md — integração real Pedidos ↔ Catálogo (sem injeção de falha). */
@Tag("api")
@Tag("integration")
@DisplayName("Integração — Pedidos ↔ Catálogo")
class CatalogIntegrationTest {

    @Test
    @Tag("P1")
    @DisplayName("CT-INT-001 — pedido usa nome e preço vigentes do catálogo")
    void orderUsesCurrentCatalogData() {
        List<String> productIds = List.of(NOTEBOOK, MOUSE, KEYBOARD);
        OrderPayload payload = OrderPayload.of(TestClient.unique());
        productIds.forEach(id -> payload.item(id, 1));

        JsonPath order = Orders.createAccepted(payload).jsonPath();

        for (String id : productIds) {
            JsonPath product = Api.products().get("/{id}", id).then().statusCode(200).extract().jsonPath();
            String item = "items.find { it.productId == '" + id + "' }";
            assertThat(order.getString(item + ".productName")).as("nome de %s", id).isEqualTo(product.getString("name"));
            assertAmount(order.get(item + ".unitPrice"), ((BigDecimal) product.get("currentPrice.value")).toPlainString(),
                    "preço de " + id);
        }
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-INT-012 — pedido com 51 produtos distintos (acima do lote do catálogo) → 422, nunca 503")
    @KnownDefect("H-01")
    void fiftyOneDistinctProducts() {
        TestClient client = TestClient.unique();
        OrderPayload payload = OrderPayload.of(client).item(NOTEBOOK, 1).item(MOUSE, 1).item(KEYBOARD, 1);
        List<String> missing = IntStream.rangeClosed(1, 48).mapToObj(Ids::missingProduct).toList();
        missing.forEach(id -> payload.item(id, 1));

        Response response = Orders.create(payload.build());

        assertError(response, 422, "PRODUCT_NOT_FOUND");
        assertThat(response.jsonPath().getList("error.details", String.class)).containsExactlyInAnyOrderElementsOf(missing);
        assertNothingPersisted(client.email());
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-INT-013 — jornada de API: criar → detalhar → listar → filtrar com valores idênticos")
    void apiJourney() {
        Response created = Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(NOTEBOOK, 1).item(MOUSE, 2));
        String id = created.jsonPath().getString("id");

        JsonPath detail = Orders.get(id).then().statusCode(200).extract().jsonPath();
        JsonPath list = Api.orders().get().then().statusCode(200).extract().jsonPath();
        JsonPath filtered = Api.orders().queryParam("status", "CRIADO").get().then().statusCode(200).extract().jsonPath();

        assertThat(list.getString("content[0].id")).as("pedido recém-criado no topo da listagem").isEqualTo(id);
        assertThat(filtered.getString("content[0].id")).as("pedido recém-criado no topo do filtro CRIADO").isEqualTo(id);
        for (String field : List.of("subtotal", "discount", "total", "status", "items", "client")) {
            Object expected = created.jsonPath().get(field);
            assertThat((Object) detail.get(field)).as("detalhe.%s", field).isEqualTo(expected);
            assertThat((Object) list.get("content[0]." + field)).as("listagem.%s", field).isEqualTo(expected);
            assertThat((Object) filtered.get("content[0]." + field)).as("filtro.%s", field).isEqualTo(expected);
        }
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-INT-014 — 20 criações concorrentes: todas aceitas, ids únicos e valores corretos")
    void concurrentCreation() throws Exception {
        int requests = 20;
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Callable<Response>> calls = IntStream.range(0, requests)
                    .<Callable<Response>>mapToObj(i -> () -> Api.ordersFromWorkerThread()
                            .body(OrderPayload.of(TestClient.unique()).item(NOTEBOOK, 1).build()).post())
                    .toList();
            List<Response> responses = pool.invokeAll(calls).stream().map(CatalogIntegrationTest::await).toList();

            assertThat(responses).extracting(Response::statusCode).as("status de todas as criações").containsOnly(201);
            List<String> ids = responses.stream().map(r -> r.jsonPath().getString("id")).toList();
            assertThat(ids).doesNotHaveDuplicates().hasSize(requests);
            for (Response response : responses) {
                Map<String, Object> fetched = Orders.get(response.jsonPath().getString("id")).then().statusCode(200)
                        .extract().jsonPath().getMap("$");
                assertAmount(fetched.get("total"), "3150.00", "total de cada pedido concorrente");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static Response await(Future<Response> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException("Falha em requisição concorrente", e);
        }
    }
}
