package com.stefanini.qa.api.catalog;

import static com.stefanini.qa.api.data.Products.CURRENCY;
import static com.stefanini.qa.api.data.Products.HEADSET_EXPIRED_PRICE;
import static com.stefanini.qa.api.data.Products.KEYBOARD;
import static com.stefanini.qa.api.data.Products.KEYBOARD_PRICE;
import static com.stefanini.qa.api.data.Products.KEYBOARD_PRICE_VALID_FROM;
import static com.stefanini.qa.api.data.Products.MISSING;
import static com.stefanini.qa.api.data.Products.MONITOR_INACTIVE;
import static com.stefanini.qa.api.data.Products.MONITOR_PRICE;
import static com.stefanini.qa.api.data.Products.NOTEBOOK;
import static com.stefanini.qa.api.data.Products.NOTEBOOK_NAME;
import static com.stefanini.qa.api.data.Products.NOTEBOOK_PRICE;
import static com.stefanini.qa.api.data.Products.SEED_VALID_FROM;
import static com.stefanini.qa.api.data.Products.WEBCAM_FUTURE_PRICE;
import static com.stefanini.qa.api.support.ApiAssertions.assertClientError;
import static com.stefanini.qa.api.support.ApiAssertions.assertError;
import static com.stefanini.qa.api.support.Money.assertAmount;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.support.Api;
import com.stefanini.qa.api.support.ApiAssertions;
import com.stefanini.qa.api.support.KnownDefect;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** docs/test-cases/api-catalog.md — A. Consulta individual (GET /api/v1/products/{id}). */
@Tag("api")
@DisplayName("Catálogo — consulta individual")
class GetProductTest {

    /** A partir desta data a massa muda (PROD-005 ganha preço vigente): risco temporal RP-07. */
    private static final Instant SEED_FUTURE_PRICE_START = Instant.parse("2030-01-01T00:00:00Z");

    @BeforeAll
    static void seedIsStillValid() {
        assertThat(Instant.now()).as("massa inicial válida até 2030-01-01 (RP-07): revisar PROD-005 e os casos afetados")
                .isBefore(SEED_FUTURE_PRICE_START);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-CAT-001 — produto ativo com preço vigente (validTo presente com valor null)")
    @KnownDefect(value = "8", suspicion = "H-09")
    void activeProductWithCurrentPrice() {
        Response response = get(NOTEBOOK);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonPath product = response.jsonPath();
        assertThat(product.getString("id")).isEqualTo(NOTEBOOK);
        assertThat(product.getString("name")).isEqualTo(NOTEBOOK_NAME);
        assertThat(product.getString("status")).isEqualTo("ATIVO");
        assertAmount(product.get("currentPrice.value"), NOTEBOOK_PRICE, "currentPrice.value");
        assertThat(product.getString("currentPrice.currency")).isEqualTo(CURRENCY);
        assertThat(product.getString("currentPrice.validFrom")).isEqualTo(SEED_VALID_FROM);
        Map<String, Object> currentPrice = product.getMap("currentPrice");
        assertThat(currentPrice).as("contrato documenta \"validTo\": null (H-09)").containsEntry("validTo", null);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-CAT-002 — produto com histórico de preços retorna a janela vigente")
    void productWithPriceHistory() {
        JsonPath product = get(KEYBOARD).then().statusCode(200).extract().jsonPath();

        assertAmount(product.get("currentPrice.value"), KEYBOARD_PRICE, "currentPrice.value");
        assertThat(product.getString("currentPrice.validFrom")).isEqualTo(KEYBOARD_PRICE_VALID_FROM);
        assertThat(product.getString("currentPrice.validTo")).isNull();
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-CAT-003 — produto inativo continua consultável")
    void inactiveProductIsStillReadable() {
        JsonPath product = get(MONITOR_INACTIVE).then().statusCode(200).extract().jsonPath();

        assertThat(product.getString("status")).isEqualTo("INATIVO");
        assertAmount(product.get("currentPrice.value"), MONITOR_PRICE, "currentPrice.value");
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-CAT-004 — preço apenas futuro: currentPrice presente com valor null")
    @KnownDefect(value = "8", suspicion = "H-09")
    void futurePriceOnly() {
        assertCurrentPriceIsExplicitNull(WEBCAM_FUTURE_PRICE);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-CAT-005 — preço expirado: currentPrice presente com valor null")
    @KnownDefect(value = "8", suspicion = "H-09")
    void expiredPrice() {
        assertCurrentPriceIsExplicitNull(HEADSET_EXPIRED_PRICE);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-CAT-006 — produto inexistente → 404 PRODUCT_NOT_FOUND")
    void missingProduct() {
        assertError(get(MISSING), 404, "PRODUCT_NOT_FOUND");
    }

    @ParameterizedTest(name = "CT-CAT-007 — identificador incomum \"{0}\" → 4xx com envelope, nunca 500")
    @ValueSource(strings = {"PROD%20001", "%2E%2E%2F", "prod-001", "LONG"})
    @Tag("P3")
    @KnownDefect(value = "11", suspicion = "H-12")
    void unusualIdentifiers(String rawId) {
        String id = "LONG".equals(rawId) ? "X".repeat(1000) : rawId;

        Response response = Api.products().urlEncodingEnabled(false).get("/" + id);

        assertClientError(response, 400, 404);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-CAT-008 — contrato da consulta individual (currentPrice e validTo obrigatórios)")
    @KnownDefect(value = "8", suspicion = "H-09")
    void singleProductContract() {
        for (String id : new String[] {NOTEBOOK, WEBCAM_FUTURE_PRICE}) {
            Response response = get(id);
            assertThat(response.contentType()).startsWith("application/json");
            ApiAssertions.assertMatchesSchema(response, "product.json");
        }
    }

    private static Response get(String id) {
        return Api.products().get("/{id}", id);
    }

    private static void assertCurrentPriceIsExplicitNull(String productId) {
        Response response = get(productId);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getMap("$"))
                .as("contrato: \"currentPrice é null quando não há janela vigente\" (sut/docs/api.md, H-09)")
                .containsEntry("currentPrice", null);
    }
}
