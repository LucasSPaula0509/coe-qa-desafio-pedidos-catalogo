package com.stefanini.qa.api.orders;

import static com.stefanini.qa.api.data.Products.HEADSET_EXPIRED_PRICE;
import static com.stefanini.qa.api.data.Products.MISSING;
import static com.stefanini.qa.api.data.Products.MONITOR_INACTIVE;
import static com.stefanini.qa.api.data.Products.NOTEBOOK;
import static com.stefanini.qa.api.data.Products.WEBCAM_FUTURE_PRICE;
import static com.stefanini.qa.api.support.ApiAssertions.assertDetailsExactly;
import static com.stefanini.qa.api.support.ApiAssertions.assertError;
import static com.stefanini.qa.api.support.ApiAssertions.assertErrorEnvelope;
import static com.stefanini.qa.api.support.ApiAssertions.assertNothingPersisted;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.data.OrderPayload;
import com.stefanini.qa.api.data.TestClient;
import com.stefanini.qa.api.support.Orders;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * docs/test-cases/api-orders.md — C. Validação dos produtos no catálogo (422). Todo caso verifica
 * no PostgreSQL que nada foi persistido (all-or-nothing, RN-07).
 */
@Tag("api")
@Tag("db")
@DisplayName("Pedidos — validação dos produtos no catálogo")
class CatalogValidationTest {

    @ParameterizedTest(name = "{0} — {1}: {2} → 422 {3}")
    @CsvSource({
        "CT-ORD-050, produto inexistente, PROD-999, PRODUCT_NOT_FOUND",
        "CT-ORD-051, produto inativo, PROD-004, PRODUCT_INACTIVE",
        "CT-ORD-052, preço apenas futuro, PROD-005, NO_CURRENT_PRICE",
        "CT-ORD-053, preço expirado, PROD-006, NO_CURRENT_PRICE"})
    @Tag("P1")
    void invalidProductRejectsOrder(String caseId, String scenario, String productId, String code) {
        TestClient client = TestClient.unique();

        Response response = Orders.create(OrderPayload.of(client).item(productId, 1).build());

        assertError(response, 422, code);
        assertDetailsExactly(response, productId);
        assertNothingPersisted(client.email());
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-054 — item válido + item inválido: rejeição total, sem persistência parcial")
    void validAndInvalidItemsRejectWholeOrder() {
        TestClient client = TestClient.unique();

        Response response = Orders.create(OrderPayload.of(client).item(NOTEBOOK, 1).item(MISSING, 1).build());

        assertError(response, 422, "PRODUCT_NOT_FOUND");
        assertDetailsExactly(response, MISSING);
        assertNothingPersisted(client.email());
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-055 — vários inválidos do mesmo tipo são todos reportados")
    void severalInvalidProductsOfSameKind() {
        TestClient client = TestClient.unique();

        Response response = Orders.create(OrderPayload.of(client)
                .item(WEBCAM_FUTURE_PRICE, 1).item(HEADSET_EXPIRED_PRICE, 1).build());

        assertError(response, 422, "NO_CURRENT_PRICE");
        assertDetailsExactly(response, WEBCAM_FUTURE_PRICE, HEADSET_EXPIRED_PRICE);
        assertNothingPersisted(client.email());
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-056 — inválidos de tipos diferentes: código aplicável e details coerente (Q-01)")
    void invalidProductsOfDifferentKinds() {
        TestClient client = TestClient.unique();
        Map<String, String> productByCode = Map.of(
                "PRODUCT_NOT_FOUND", MISSING,
                "PRODUCT_INACTIVE", MONITOR_INACTIVE,
                "NO_CURRENT_PRICE", WEBCAM_FUTURE_PRICE);

        Response response = Orders.create(OrderPayload.of(client)
                .item(MISSING, 1).item(MONITOR_INACTIVE, 1).item(WEBCAM_FUTURE_PRICE, 1).build());

        assertThat(response.statusCode()).as("corpo: %s", response.asString()).isEqualTo(422);
        assertErrorEnvelope(response);
        String code = response.jsonPath().getString("error.code");
        assertThat(productByCode).as("código aplicável").containsKey(code);
        assertDetailsExactly(response, productByCode.get(code));
        assertNothingPersisted(client.email());
        Allure.addAttachment("Q-01 — precedência observada", "Código devolvido para {inexistente, inativo, sem preço}: " + code);
    }
}
