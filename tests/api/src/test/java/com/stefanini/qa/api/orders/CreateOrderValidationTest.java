package com.stefanini.qa.api.orders;

import static com.stefanini.qa.api.data.OrderPayload.ABSENT;
import static com.stefanini.qa.api.data.Products.KEYBOARD;
import static com.stefanini.qa.api.data.Products.MISSING;
import static com.stefanini.qa.api.data.Products.MOUSE;
import static com.stefanini.qa.api.data.Products.NOTEBOOK;
import static com.stefanini.qa.api.support.ApiAssertions.assertClientError;
import static com.stefanini.qa.api.support.ApiAssertions.assertDetailsContain;
import static com.stefanini.qa.api.support.ApiAssertions.assertError;
import static com.stefanini.qa.api.support.ApiAssertions.assertNothingPersisted;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stefanini.qa.api.data.OrderPayload;
import com.stefanini.qa.api.data.TestClient;
import com.stefanini.qa.api.support.Api;
import com.stefanini.qa.api.support.Ids;
import com.stefanini.qa.api.support.Orders;
import com.stefanini.qa.api.support.KnownDefect;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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

/**
 * docs/test-cases/api-orders.md — B. Validação de entrada (400). Todo caso verifica também o
 * envelope de erro, o X-Request-Id e a ausência de persistência para o cliente do teste.
 */
@Tag("api")
@DisplayName("Pedidos — validação de entrada")
class CreateOrderValidationTest {

    private static final int ONE_MEBIBYTE = 1_048_576;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ----- Cliente ------------------------------------------------------------------------------

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-020 — cliente ausente")
    void missingClient() {
        TestClient client = TestClient.unique();
        Response response = Orders.create(OrderPayload.of(client).item(MOUSE, 1).rootField("client", ABSENT).build());

        assertValidationError(response, "client", client);
    }

    static Stream<Arguments> blankValues() {
        return Stream.of(
                Arguments.of("ausente", ABSENT),
                Arguments.of("null", null),
                Arguments.of("vazio", ""),
                Arguments.of("só espaços", "   "));
    }

    @ParameterizedTest(name = "CT-ORD-021 — nome inválido: {0}")
    @MethodSource("blankValues")
    @Tag("P1")
    void invalidName(String variation, Object value) {
        assertInvalidClientField("name", value);
    }

    @ParameterizedTest(name = "CT-ORD-022 — documento inválido: {0}")
    @MethodSource("blankValues")
    @Tag("P1")
    void invalidDocument(String variation, Object value) {
        assertInvalidClientField("document", value);
    }

    @ParameterizedTest(name = "CT-ORD-023 — e-mail ausente ou vazio: {0}")
    @MethodSource("blankValues")
    @Tag("P1")
    void blankEmail(String variation, Object value) {
        TestClient client = TestClient.unique();
        Response response = Orders.create(OrderPayload.of(client).clientField("email", value).item(MOUSE, 1).build());

        assertError(response, 400, "VALIDATION_ERROR");
        assertDetailsContain(response, "client.email");
    }

    @ParameterizedTest(name = "CT-ORD-024 — e-mail com formato inválido: \"{0}\"")
    @ValueSource(strings = {"maria", "maria@", "@example.com", "maria silva@example.com", "maria@@example.com"})
    @Tag("P1")
    void malformedEmail(String email) {
        Response response = Orders.create(OrderPayload.of(TestClient.unique())
                .clientField("email", email).item(MOUSE, 1).build());

        assertError(response, 400, "VALIDATION_ERROR");
        assertDetailsContain(response, "client.email");
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-025 — e-mail válido com subendereço e subdomínio é aceito")
    void emailWithPlusTagAndSubdomainIsAccepted() {
        TestClient base = TestClient.unique();
        String email = base.email().replace("@example.com", "+tag@sub.example.com");

        Response response = Orders.create(OrderPayload.of(base).clientField("email", email).item(MOUSE, 1).build());

        assertThat(response.statusCode()).as("corpo: %s", response.asString()).isEqualTo(201);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-026 — limite do nome: 120 aceito, 121 rejeitado")
    void nameLengthBoundary() {
        assertFieldLengthBoundary("name", 120);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-027 — limite do documento: 20 aceito, 21 rejeitado")
    void documentLengthBoundary() {
        assertFieldLengthBoundary("document", 20);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-028 — limite do e-mail: 120 aceito, 121 rejeitado")
    void emailLengthBoundary() {
        TestClient client = TestClient.unique();
        String atLimit = emailOfLength(120);
        String aboveLimit = emailOfLength(121);

        Response accepted = Orders.create(OrderPayload.of(client).clientField("email", atLimit).item(MOUSE, 1).build());
        Response rejected = Orders.create(OrderPayload.of(client).clientField("email", aboveLimit).item(MOUSE, 1).build());

        assertThat(accepted.statusCode()).as("e-mail com 120 caracteres — corpo: %s", accepted.asString()).isEqualTo(201);
        assertError(rejected, 400, "VALIDATION_ERROR");
        assertDetailsContain(rejected, "client.email");
        assertNothingPersisted(aboveLimit);
    }

    // ----- Itens --------------------------------------------------------------------------------

    static Stream<Arguments> emptyItems() {
        return Stream.of(Arguments.of("ausente", ABSENT), Arguments.of("null", null), Arguments.of("vazio", List.of()));
    }

    @ParameterizedTest(name = "CT-ORD-030 — pedido sem itens: {0}")
    @MethodSource("emptyItems")
    @Tag("P1")
    void orderWithoutItems(String variation, Object items) {
        TestClient client = TestClient.unique();
        Response response = Orders.create(OrderPayload.of(client).rootField("items", items).build());

        assertValidationError(response, "items", client);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-031 — 101 itens distintos excedem o limite")
    void moreThanOneHundredItems() {
        TestClient client = TestClient.unique();
        OrderPayload payload = OrderPayload.of(client);
        IntStream.rangeClosed(1, 101).forEach(i -> payload.item(Ids.missingProduct(i), 1));

        Response response = Orders.create(payload.build());

        assertValidationError(response, "items", client);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-032 — 100 itens distintos passam na validação (422 pelos inexistentes, nunca 5xx)")
    @KnownDefect(value = "4", suspicion = "H-01")
    void oneHundredItemsAreAcceptedByValidation() {
        assertProductNotFoundForMissing(97);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-033 — 50 itens distintos (limite do lote do catálogo) → 422 pelos inexistentes")
    void fiftyItemsReportMissingProducts() {
        assertProductNotFoundForMissing(47);
    }

    @ParameterizedTest(name = "CT-ORD-034 — quantidade {0} → {1}")
    @MethodSource("quantityBoundaries")
    @Tag("P1")
    void quantityBoundaries(int quantity, int expectedStatus) {
        TestClient client = TestClient.unique();
        Response response = Orders.create(OrderPayload.of(client).item(MOUSE, quantity).build());

        if (expectedStatus == 201) {
            assertThat(response.statusCode()).as("corpo: %s", response.asString()).isEqualTo(201);
            assertThat(response.jsonPath().getInt("items[0].quantity")).isEqualTo(quantity);
        } else {
            assertError(response, 400, "VALIDATION_ERROR");
            assertDetailsContain(response, "quantity");
            assertNothingPersisted(client.email());
        }
    }

    static Stream<Arguments> quantityBoundaries() {
        return Stream.of(
                Arguments.of(-1, 400), Arguments.of(0, 400), Arguments.of(1, 201),
                Arguments.of(999, 201), Arguments.of(1000, 400));
    }

    @ParameterizedTest(name = "CT-ORD-035 — quantidade {0}")
    @MethodSource("missingQuantity")
    @Tag("P1")
    void missingQuantity(String variation, Object quantity) {
        TestClient client = TestClient.unique();
        Response response = Orders.create(OrderPayload.of(client).item(MOUSE, quantity).build());

        assertValidationError(response, "quantity", client);
    }

    static Stream<Arguments> missingQuantity() {
        return Stream.of(Arguments.of("ausente", ABSENT), Arguments.of("null", null));
    }

    @ParameterizedTest(name = "CT-ORD-036 — quantidade com tipo não inteiro: {0}")
    @MethodSource("nonIntegerQuantities")
    @Tag("P2")
    @KnownDefect(value = "7", suspicion = "H-03")
    void nonIntegerQuantityIsRejected(String variation, Object quantity) {
        TestClient client = TestClient.unique();
        Response response = Orders.create(OrderPayload.of(client).item(MOUSE, quantity).build());

        assertError(response, 400, "VALIDATION_ERROR");
        assertNothingPersisted(client.email());
    }

    static Stream<Arguments> nonIntegerQuantities() {
        return Stream.of(
                Arguments.of("decimal 1.5", 1.5),
                Arguments.of("texto \"2\"", "2"),
                Arguments.of("booleano true", true),
                Arguments.of("texto \"abc\"", "abc"));
    }

    @ParameterizedTest(name = "CT-ORD-037 — productId inválido: {0}")
    @MethodSource("blankValues")
    @Tag("P1")
    void invalidProductId(String variation, Object productId) {
        TestClient client = TestClient.unique();
        Response response = Orders.create(OrderPayload.of(client).item(productIdItem(productId)).build());

        assertValidationError(response, "productId", client);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-038 — limite do productId: 50 passa a validação (422), 51 é rejeitado (400)")
    void productIdLengthBoundary() {
        String atLimit = "QA-" + "X".repeat(47);
        String aboveLimit = atLimit + "X";

        Response accepted = Orders.create(OrderPayload.of(TestClient.unique()).item(atLimit, 1).build());
        Response rejected = Orders.create(OrderPayload.of(TestClient.unique()).item(aboveLimit, 1).build());

        assertError(accepted, 422, "PRODUCT_NOT_FOUND");
        assertDetailsContain(accepted, atLimit);
        assertError(rejected, 400, "VALIDATION_ERROR");
        assertDetailsContain(rejected, "productId");
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-039 — produto repetido no pedido é rejeitado")
    void duplicatedProductIsRejected() {
        TestClient client = TestClient.unique();
        Response response = Orders.create(OrderPayload.of(client).item(NOTEBOOK, 1).item(NOTEBOOK, 2).build());

        assertError(response, 400, "VALIDATION_ERROR");
        assertDetailsContain(response, "duplicate product PROD-001");
        assertNothingPersisted(client.email());
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-040 — repetição tem precedência sobre a validação de catálogo (400, não 422)")
    void duplicateValidationPrecedesCatalogValidation() {
        Response response = Orders.create(OrderPayload.of(TestClient.unique()).item(MISSING, 1).item(MISSING, 1).build());

        assertError(response, 400, "VALIDATION_ERROR");
        assertDetailsContain(response, "duplicate product " + MISSING);
    }

    @Test
    @Tag("P3")
    @DisplayName("CT-ORD-041 — IDs diferenciam maiúsculas e minúsculas (não são duplicados)")
    void productIdsAreCaseSensitive() {
        Response response = Orders.create(OrderPayload.of(TestClient.unique()).item(NOTEBOOK, 1).item("prod-001", 1).build());

        assertError(response, 422, "PRODUCT_NOT_FOUND");
        assertDetailsContain(response, "prod-001");
    }

    // ----- Corpo da requisição ------------------------------------------------------------------

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-042 — JSON malformado")
    void malformedJson() {
        Response response = Api.orders().body("{\"client\": {").post();

        assertError(response, 400, "VALIDATION_ERROR");
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-043 — corpo vazio")
    void emptyBody() {
        Response response = Api.orders().body("").post();

        assertError(response, 400, "VALIDATION_ERROR");
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-044 — Content-Type não suportado (text/plain) → 4xx com envelope, nunca 500")
    @KnownDefect(value = "6", suspicion = "H-02")
    void unsupportedContentType() {
        TestClient client = TestClient.unique();
        Response response = Api.orders().contentType("text/plain")
                .body(json(OrderPayload.of(client).item(MOUSE, 1))).post();

        assertClientError(response, 400, 415);
        assertNothingPersisted(client.email());
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-045 — corpo de exatamente 1 MB (1.048.576 bytes) é aceito")
    void bodyOfExactlyOneMebibyteIsAccepted() {
        TestClient client = TestClient.unique();
        byte[] body = paddedTo(json(OrderPayload.of(client).item(MOUSE, 1)), ONE_MEBIBYTE);

        Response response = Api.orders().body(body).post();

        assertThat(body).hasSize(ONE_MEBIBYTE);
        assertThat(response.statusCode()).as("corpo: %s", response.asString()).isEqualTo(201);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-046 — corpo acima de 1 MB (1.048.577 bytes) → 413 PAYLOAD_TOO_LARGE")
    void bodyAboveOneMebibyteIsRejected() {
        TestClient client = TestClient.unique();
        byte[] body = paddedTo(json(OrderPayload.of(client).item(MOUSE, 1)), ONE_MEBIBYTE + 1);

        Response response = Api.orders().body(body).post();

        assertError(response, 413, "PAYLOAD_TOO_LARGE");
        assertNothingPersisted(client.email());
    }

    // ----- Apoio --------------------------------------------------------------------------------

    private static void assertInvalidClientField(String field, Object value) {
        TestClient client = TestClient.unique();
        Response response = Orders.create(OrderPayload.of(client).clientField(field, value).item(MOUSE, 1).build());

        assertValidationError(response, "client." + field, client);
    }

    private static void assertValidationError(Response response, String detailFragment, TestClient client) {
        assertError(response, 400, "VALIDATION_ERROR");
        assertDetailsContain(response, detailFragment);
        assertNothingPersisted(client.email());
    }

    private static void assertFieldLengthBoundary(String field, int maxLength) {
        TestClient client = TestClient.unique();
        String prefix = client.email().substring(3, 15) + " ";
        String atLimit = prefix + "x".repeat(maxLength - prefix.length());
        String aboveLimit = atLimit + "x";

        Response accepted = Orders.create(OrderPayload.of(client).clientField(field, atLimit).item(MOUSE, 1).build());
        Response rejected = Orders.create(OrderPayload.of(client).clientField(field, aboveLimit).item(MOUSE, 1).build());

        assertThat(accepted.statusCode()).as("%s com %d caracteres — corpo: %s", field, maxLength, accepted.asString())
                .isEqualTo(201);
        assertThat(accepted.jsonPath().getString("client." + field)).isEqualTo(atLimit);
        assertError(rejected, 400, "VALIDATION_ERROR");
        assertDetailsContain(rejected, "client." + field);
    }

    /** 3 produtos válidos + N inexistentes: o limite de itens não rejeita; o catálogo aponta os inexistentes. */
    private static void assertProductNotFoundForMissing(int missingCount) {
        TestClient client = TestClient.unique();
        OrderPayload payload = OrderPayload.of(client).item(NOTEBOOK, 1).item(MOUSE, 1).item(KEYBOARD, 1);
        List<String> missing = IntStream.rangeClosed(1, missingCount).mapToObj(Ids::missingProduct).toList();
        missing.forEach(id -> payload.item(id, 1));

        Response response = Orders.create(payload.build());

        assertError(response, 422, "PRODUCT_NOT_FOUND");
        assertThat(response.jsonPath().getList("error.details", String.class))
                .as("IDs inexistentes em details").containsExactlyInAnyOrderElementsOf(missing);
        assertNothingPersisted(client.email());
    }

    /** E-mail válido (parte local ≤ 64, rótulos ≤ 63, RFC 5321) com exatamente {@code length} caracteres. */
    static String emailOfLength(int length) {
        String local = "qa." + Ids.runId();
        String tld = ".com";
        int labelsLength = length - local.length() - 1 - tld.length();
        StringBuilder domain = new StringBuilder();
        while (labelsLength > 0) {
            int label = Math.min(labelsLength, 60);
            if (labelsLength - label == 1) {
                label--;
            }
            domain.append("d".repeat(label));
            labelsLength -= label;
            if (labelsLength > 0) {
                domain.append('.');
                labelsLength--;
            }
        }
        String email = local + "@" + domain + tld;
        assertThat(email).hasSize(length);
        return email;
    }

    private static Map<String, Object> productIdItem(Object productId) {
        Map<String, Object> item = new LinkedHashMap<>();
        if (productId != ABSENT) {
            item.put("productId", productId);
        }
        item.put("quantity", 1);
        return item;
    }

    private static String json(OrderPayload payload) {
        try {
            return MAPPER.writeValueAsString(payload.build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Completa o JSON com espaços em branco (insignificantes para o parser) até o tamanho exato. */
    private static byte[] paddedTo(String json, int size) {
        byte[] content = json.getBytes(StandardCharsets.UTF_8);
        int padding = size - content.length;
        assertThat(padding).as("payload base menor que o tamanho alvo").isPositive();
        return (json.substring(0, json.length() - 1) + " ".repeat(padding) + "}").getBytes(StandardCharsets.UTF_8);
    }
}
