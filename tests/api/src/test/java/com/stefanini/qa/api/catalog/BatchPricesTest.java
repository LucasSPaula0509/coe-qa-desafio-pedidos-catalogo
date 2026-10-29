package com.stefanini.qa.api.catalog;

import static com.stefanini.qa.api.data.Products.HEADSET_EXPIRED_PRICE;
import static com.stefanini.qa.api.data.Products.KEYBOARD;
import static com.stefanini.qa.api.data.Products.KEYBOARD_PRICE;
import static com.stefanini.qa.api.data.Products.MISSING;
import static com.stefanini.qa.api.data.Products.MONITOR_INACTIVE;
import static com.stefanini.qa.api.data.Products.MONITOR_PRICE;
import static com.stefanini.qa.api.data.Products.MOUSE;
import static com.stefanini.qa.api.data.Products.MOUSE_NAME;
import static com.stefanini.qa.api.data.Products.MOUSE_PRICE;
import static com.stefanini.qa.api.data.Products.NOTEBOOK;
import static com.stefanini.qa.api.data.Products.NOTEBOOK_NAME;
import static com.stefanini.qa.api.data.Products.NOTEBOOK_PRICE;
import static com.stefanini.qa.api.data.Products.WEBCAM_FUTURE_PRICE;
import static com.stefanini.qa.api.support.ApiAssertions.assertClientError;
import static com.stefanini.qa.api.support.ApiAssertions.assertError;
import static com.stefanini.qa.api.support.Money.assertAmount;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.support.Api;
import com.stefanini.qa.api.support.ApiAssertions;
import com.stefanini.qa.api.support.Ids;
import com.stefanini.qa.api.support.KnownDefect;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** docs/test-cases/api-catalog.md — B. Consulta em lote (POST /api/v1/products/prices). */
@Tag("api")
@DisplayName("Catálogo — consulta em lote")
class BatchPricesTest {

    private static final List<String> ALL_SEED_PRODUCTS = List.of(
            NOTEBOOK, MOUSE, KEYBOARD, MONITOR_INACTIVE, WEBCAM_FUTURE_PRICE, HEADSET_EXPIRED_PRICE);

    @Test
    @Tag("P1")
    @DisplayName("CT-CAT-010 — lote de produtos válidos")
    void validBatch() {
        JsonPath body = prices(List.of(NOTEBOOK, MOUSE)).then().statusCode(200).extract().jsonPath();

        assertThat(body.getList("prices")).hasSize(2);
        assertThat(body.getString("prices.find { it.productId == 'PROD-001' }.name")).isEqualTo(NOTEBOOK_NAME);
        assertAmount(body.get("prices.find { it.productId == 'PROD-001' }.currentPrice.value"), NOTEBOOK_PRICE, "PROD-001");
        assertThat(body.getString("prices.find { it.productId == 'PROD-002' }.name")).isEqualTo(MOUSE_NAME);
        assertAmount(body.get("prices.find { it.productId == 'PROD-002' }.currentPrice.value"), MOUSE_PRICE, "PROD-002");
        assertThat(body.getList("notFound")).isEmpty();
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-CAT-011 — lote com todos os cenários da massa + inexistente")
    void batchWithAllSeedScenarios() {
        List<String> ids = new ArrayList<>(ALL_SEED_PRODUCTS);
        ids.add(MISSING);

        JsonPath body = prices(ids).then().statusCode(200).extract().jsonPath();

        List<Map<String, Object>> entries = body.getList("prices");
        Map<String, Map<String, Object>> byId = new HashMap<>();
        entries.forEach(entry -> byId.put((String) entry.get("productId"), entry));
        assertThat(byId).containsOnlyKeys(ALL_SEED_PRODUCTS);
        assertThat(byId.get(MONITOR_INACTIVE)).containsEntry("status", "INATIVO");
        assertAmount(body.get("prices.find { it.productId == 'PROD-003' }.currentPrice.value"), KEYBOARD_PRICE, "PROD-003");
        assertAmount(body.get("prices.find { it.productId == 'PROD-004' }.currentPrice.value"), MONITOR_PRICE, "PROD-004");
        assertThat(body.getString("prices.find { it.productId == 'PROD-005' }.currentPrice")).as("PROD-005 sem preço").isNull();
        assertThat(body.getString("prices.find { it.productId == 'PROD-006' }.currentPrice")).as("PROD-006 sem preço").isNull();
        assertThat(body.getList("notFound", String.class)).containsExactly(MISSING);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-CAT-012 — lote só com inexistentes")
    void batchWithOnlyMissingProducts() {
        JsonPath body = prices(List.of("PROD-998", MISSING)).then().statusCode(200).extract().jsonPath();

        assertThat(body.getList("prices")).isEmpty();
        assertThat(body.getList("notFound", String.class)).containsExactlyInAnyOrder("PROD-998", MISSING);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-CAT-013 — limite inferior do lote (1 ID)")
    void singleIdBatch() {
        JsonPath body = prices(List.of(NOTEBOOK)).then().statusCode(200).extract().jsonPath();

        assertThat(body.getList("prices")).hasSize(1);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-CAT-014 — limite superior do lote (50 IDs)")
    void fiftyIdBatch() {
        List<String> ids = new ArrayList<>(ALL_SEED_PRODUCTS);
        IntStream.rangeClosed(1, 44).forEach(i -> ids.add(Ids.missingProduct(i)));

        JsonPath body = prices(ids).then().statusCode(200).extract().jsonPath();

        assertThat(body.getList("prices")).hasSize(6);
        assertThat(body.getList("notFound")).hasSize(44);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-CAT-015 — acima do limite do lote (51 IDs) → 400")
    void fiftyOneIdBatch() {
        List<String> ids = IntStream.rangeClosed(1, 51).mapToObj(Ids::missingProduct).toList();

        assertError(prices(ids), 400, "VALIDATION_ERROR");
    }

    static Stream<Arguments> emptyBatches() {
        return Stream.of(
                Arguments.of("lista vazia", "{\"productIds\": []}"),
                Arguments.of("null", "{\"productIds\": null}"),
                Arguments.of("campo ausente", "{}"));
    }

    @ParameterizedTest(name = "CT-CAT-016 — lote vazio ou ausente: {0} → 400")
    @MethodSource("emptyBatches")
    @Tag("P1")
    void emptyBatch(String variation, String body) {
        assertError(Api.products().body(body).post("/prices"), 400, "VALIDATION_ERROR");
    }

    @ParameterizedTest(name = "CT-CAT-017 — ID em branco no lote: \"{0}\" → 400")
    @ValueSource(strings = {"", "   "})
    @Tag("P2")
    void blankIdInBatch(String blank) {
        assertError(prices(Arrays.asList(NOTEBOOK, blank)), 400, "VALIDATION_ERROR");
    }

    @Test
    @Tag("P3")
    @DisplayName("CT-CAT-018 — IDs duplicados no lote → 400 (Q-04)")
    @KnownDefect(value = "9", suspicion = "H-06")
    void duplicatedIdsInBatch() {
        assertError(prices(List.of(NOTEBOOK, NOTEBOOK)), 400, "VALIDATION_ERROR");
    }

    static Stream<Arguments> invalidTypes() {
        return Stream.of(
                Arguments.of("texto em vez de lista", "{\"productIds\": \"PROD-001\"}"),
                Arguments.of("números", "{\"productIds\": [1, 2]}"),
                Arguments.of("objeto de operador NoSQL", "{\"productIds\": [{\"$ne\": null}]}"));
    }

    @ParameterizedTest(name = "CT-CAT-019 — tipo inválido no lote: {0} → 400")
    @MethodSource("invalidTypes")
    @Tag("P2")
    @KnownDefect(value = "7", suspicion = "H-03")
    void invalidTypesInBatch(String variation, String body) {
        Response response = Api.products().body(body).post("/prices");

        assertError(response, 400, "VALIDATION_ERROR");
        assertThat(response.asString()).as("nunca devolver produtos").doesNotContain("\"prices\"");
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-CAT-020 — JSON malformado no lote → 400")
    void malformedBatch() {
        assertError(Api.products().body("{\"productIds\": [").post("/prices"), 400, "VALIDATION_ERROR");
    }

    static Stream<Arguments> writeAttempts() {
        return Stream.of(
                Arguments.of("POST", ""), Arguments.of("PUT", "/PROD-001"),
                Arguments.of("PATCH", "/PROD-001"), Arguments.of("DELETE", "/PROD-001"));
    }

    @ParameterizedTest(name = "CT-CAT-021 — catálogo somente leitura: {0} /products{1} → 405/404 com envelope")
    @MethodSource("writeAttempts")
    @Tag("P2")
    @KnownDefect(value = "6", suspicion = "H-02")
    void catalogIsReadOnly(String method, String path) {
        Response response = Api.products().body("{\"name\": \"Alterado\"}").request(method, path);

        assertClientError(response, 404, 405);
        JsonPath product = Api.products().get("/{id}", NOTEBOOK).jsonPath();
        assertThat(product.getString("name")).as("produto inalterado").isEqualTo(NOTEBOOK_NAME);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-CAT-022 — contrato da consulta em lote")
    @KnownDefect(value = "8", suspicion = "H-09")
    void batchContract() {
        List<String> ids = new ArrayList<>(ALL_SEED_PRODUCTS);
        ids.add(MISSING);

        ApiAssertions.assertMatchesSchema(prices(ids), "product-prices.json");
    }

    private static Response prices(List<String> productIds) {
        return Api.products().body(Map.of("productIds", productIds)).post("/prices");
    }
}
