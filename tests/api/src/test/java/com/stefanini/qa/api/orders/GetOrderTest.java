package com.stefanini.qa.api.orders;

import static com.stefanini.qa.api.support.ApiAssertions.assertClientError;
import static com.stefanini.qa.api.support.ApiAssertions.assertError;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.support.ApiAssertions;
import com.stefanini.qa.api.support.Orders;
import com.stefanini.qa.api.support.KnownDefect;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** docs/test-cases/api-orders.md — D. Consulta por identificador. */
@Tag("api")
@DisplayName("Pedidos — consulta por identificador")
class GetOrderTest {

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-060 — consulta de pedido existente retorna o pedido conforme o contrato")
    void existingOrder() {
        Response created = Orders.createSimple();

        Response response = Orders.get(created.jsonPath().getString("id"));

        assertThat(response.statusCode()).isEqualTo(200);
        ApiAssertions.assertMatchesSchema(response, "order.json");
        assertThat(response.jsonPath().getString("id")).isEqualTo(created.jsonPath().getString("id"));
        assertThat(response.jsonPath().getMap("client")).isEqualTo(created.jsonPath().getMap("client"));
        assertThat(response.jsonPath().getList("items")).isEqualTo(created.jsonPath().getList("items"));
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-061 — pedido inexistente → 404 ORDER_NOT_FOUND")
    void missingOrder() {
        Response response = Orders.get(UUID.randomUUID().toString());

        assertError(response, 404, "ORDER_NOT_FOUND");
    }

    @ParameterizedTest(name = "CT-ORD-062 — identificador com formato inválido \"{0}\" → 4xx com envelope, nunca 500")
    @ValueSource(strings = {"abc", "123", "PROD-001"})
    @Tag("P2")
    @KnownDefect(value = "6", suspicion = "H-02")
    void malformedIdentifier(String id) {
        Response response = Orders.get(id);

        assertClientError(response, 400, 404);
    }
}
