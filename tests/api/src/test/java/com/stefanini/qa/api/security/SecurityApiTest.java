package com.stefanini.qa.api.security;

import static com.stefanini.qa.api.data.Products.MISSING;
import static com.stefanini.qa.api.data.Products.MOUSE;
import static com.stefanini.qa.api.data.Products.NOTEBOOK;
import static com.stefanini.qa.api.data.Products.NOTEBOOK_PRICE;
import static com.stefanini.qa.api.support.ApiAssertions.REQUEST_ID_HEADER;
import static com.stefanini.qa.api.support.ApiAssertions.assertClientError;
import static com.stefanini.qa.api.support.ApiAssertions.assertError;
import static com.stefanini.qa.api.support.ApiAssertions.assertNoImplementationLeak;
import static com.stefanini.qa.api.support.ApiAssertions.assertNothingPersisted;
import static com.stefanini.qa.api.support.Money.assertAmount;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stefanini.qa.api.data.OrderPayload;
import com.stefanini.qa.api.data.TestClient;
import com.stefanini.qa.api.db.OrdersDatabase;
import com.stefanini.qa.api.support.Api;
import com.stefanini.qa.api.support.KnownDefect;
import com.stefanini.qa.api.support.Orders;
import com.stefanini.qa.api.support.TestConfig;
import io.qameta.allure.Allure;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * docs/test-cases/security.md — testes de segurança das APIs (OWASP API Security Top 10 2023).
 * Não destrutivos: sem inundação de requisições; entradas abusivas pontuais.
 */
@Tag("security")
@DisplayName("Segurança — APIs")
class SecurityApiTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final String UUID_V4 = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";

    @Test
    @Tag("P1")
    @DisplayName("CT-SEC-001 — preço, total, status e id forjados pelo cliente são ignorados (API3:2023)")
    void massAssignmentIsIgnored() {
        Map<String, Object> forgedItem = new LinkedHashMap<>();
        forgedItem.put("productId", NOTEBOOK);
        forgedItem.put("quantity", 1);
        forgedItem.put("unitPrice", 0.01);
        forgedItem.put("lineTotal", 0.01);
        String forgedId = UUID.randomUUID().toString();

        JsonPath order = Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(forgedItem)
                .rootField("total", 0).rootField("discount", 99999).rootField("status", "PAGO")
                .rootField("id", forgedId)).jsonPath();

        assertAmount(order.get("items[0].unitPrice"), NOTEBOOK_PRICE, "preço vem do catálogo");
        assertAmount(order.get("total"), "3150.00", "total calculado pelo sistema");
        assertThat(order.getString("status")).isEqualTo("CRIADO");
        assertThat(order.getString("id")).isNotEqualTo(forgedId);
    }

    @ParameterizedTest(name = "CT-SEC-002 — injeção SQL no filtro de status: \"{0}\" → 400, sem SQL exposto")
    @ValueSource(strings = {"CRIADO' OR '1'='1", "CRIADO;DROP TABLE orders", "' UNION SELECT * FROM orders--"})
    @Tag("P1")
    void sqlInjectionInStatusFilter(String payload) {
        long ordersBefore = OrdersDatabase.countAllOrders();

        Response response = Api.orders().queryParam("status", payload).get();

        assertError(response, 400, "VALIDATION_ERROR");
        assertThat(OrdersDatabase.countAllOrders()).as("tabela orders íntegra").isGreaterThanOrEqualTo(ordersBefore);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-SEC-002 — injeção SQL em productId e no nome do cliente: tratada como dado")
    void sqlInjectionInBodyIsTreatedAsData() {
        long ordersBefore = OrdersDatabase.countAllOrders();
        TestClient base = TestClient.unique();
        String maliciousName = "Robert'); DROP TABLE orders;-- " + base.email().substring(3, 12);

        Response unknownProduct = Orders.create(OrderPayload.of(TestClient.unique()).item("' OR 1=1--", 1).build());
        Response stored = Orders.createAccepted(OrderPayload.of(base).clientField("name", maliciousName).item(MOUSE, 1));

        assertError(unknownProduct, 422, "PRODUCT_NOT_FOUND");
        assertThat(Orders.get(stored.jsonPath().getString("id")).jsonPath().getString("client.name"))
                .as("nome armazenado literalmente").isEqualTo(maliciousName);
        assertThat(OrdersDatabase.countAllOrders()).as("tabela orders íntegra").isGreaterThan(ordersBefore);
    }

    static Stream<Arguments> noSqlInjections() {
        return Stream.of(
                Arguments.of("operador $ne no lote", "{\"productIds\": [{\"$ne\": null}]}"),
                Arguments.of("operador $gt no lugar da lista", "{\"productIds\": {\"$gt\": \"\"}}"),
                Arguments.of("operador $regex no lote", "{\"productIds\": [{\"$regex\": \".*\"}]}"));
    }

    @ParameterizedTest(name = "CT-SEC-003 — injeção NoSQL no lote: {0} → 400, sem devolver produtos")
    @MethodSource("noSqlInjections")
    @Tag("P1")
    void noSqlInjectionInBatch(String variation, String body) {
        Response response = Api.products().body(body).post("/prices");

        assertError(response, 400, "VALIDATION_ERROR");
        assertThat(response.asString()).doesNotContain("PROD-00");
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-SEC-003 — injeção NoSQL no identificador do produto → 404, sem devolver produtos")
    void noSqlInjectionInProductId() {
        String encoded = URLEncoder.encode("{\"$ne\":null}", StandardCharsets.UTF_8);

        Response response = Api.products().urlEncodingEnabled(false).get("/" + encoded);

        assertClientError(response, 400, 404);
        assertThat(response.asString()).doesNotContain("PROD-00");
    }

    @Test
    @Tag("P2")
    @KnownDefect(value = "25", suspicion = "H-04")
    @DisplayName("CT-SEC-005 — corpo acima de 1 MB com Transfer-Encoding: chunked → 413")
    void chunkedBodyAboveLimitIsRejected() throws Exception {
        TestClient client = TestClient.unique();
        String json = MAPPER.writeValueAsString(OrderPayload.of(client).item(MOUSE, 1).build());
        byte[] body = (json.substring(0, json.length() - 1) + " ".repeat(2 * 1_048_576) + "}")
                .getBytes(StandardCharsets.UTF_8);
        // InputStream sem tamanho conhecido: o cliente envia com Transfer-Encoding: chunked (sem Content-Length).
        HttpRequest request = HttpRequest.newBuilder(URI.create(TestConfig.ORDER_BASE_URL + Api.ORDERS_PATH))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(body)))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        Allure.addAttachment("Resposta ao corpo chunked de 2 MB", response.statusCode() + " " + response.body());
        assertThat(response.statusCode()).as("limite de 1 MB também com chunked (RN-06) — corpo: %s", response.body())
                .isEqualTo(413);
        assertNothingPersisted(client.email());
    }

    static Stream<Arguments> abusiveStructures() {
        String deep = "{\"client\":" + "{\"a\":".repeat(10_000) + "1" + "}".repeat(10_000) + "}";
        Map<String, Object> hugeName = OrderPayload.of(TestClient.unique()).clientField("name", "A".repeat(900 * 1024))
                .item(MOUSE, 1).build();
        Map<String, Object> hugeProductId = OrderPayload.of(TestClient.unique()).item("P".repeat(900 * 1024), 1).build();
        return Stream.of(
                Arguments.of("JSON com 10.000 níveis de aninhamento", deep),
                Arguments.of("nome com 900 KB", hugeName),
                Arguments.of("productId com 900 KB", hugeProductId));
    }

    @ParameterizedTest(name = "CT-SEC-006 — estrutura abusiva: {0} → 400 rápido, serviço saudável")
    @MethodSource("abusiveStructures")
    @Tag("P2")
    void abusiveStructuresAreRejectedQuickly(String variation, Object body) {
        // Sem o filtro do Allure: formatar um JSON de 10.000 níveis para o relatório custa dezenas de
        // segundos no cliente e mascararia o tempo de resposta do servidor (que é o que se mede aqui).
        long start = System.nanoTime();
        Response response = Api.ordersFromWorkerThread().body(body).post();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertError(response, 400, "VALIDATION_ERROR");
        assertThat(elapsed).as("rejeição rápida").isLessThan(Duration.ofSeconds(2));
        assertThat(Api.orderService().get("/actuator/health").jsonPath().getString("status")).isEqualTo("UP");
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-SEC-007 — APIs não expõem detalhes técnicos em erros nem versão em headers")
    void apisDoNotLeakTechnicalDetails() {
        List<Response> responses = List.of(
                Orders.get("abc"),
                Api.orders().body("{\"items\": [1,").post(),
                Api.orders().queryParam("page", "abc").get(),
                Api.products().body("{\"productIds\": [{}]}").post("/prices"),
                Api.products().get("/{id}", MISSING));

        for (Response response : responses) {
            assertNoImplementationLeak(response);
            assertThat(response.header("X-Powered-By")).as("X-Powered-By").isNull();
            assertThat(response.header("Server")).as("Server sem versão").satisfiesAnyOf(
                    server -> assertThat(server).isNull(), server -> assertThat(server).doesNotContainPattern("\\d"));
        }
    }

    @Test
    @Tag("P2")
    @KnownDefect(value = "28", suspicion = "H-19")
    @DisplayName("CT-SEC-007 — front-end (nginx) não expõe versão do servidor")
    void frontendDoesNotLeakServerVersion() {
        Response response = given().baseUri(TestConfig.WEB_BASE_URL).get("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.header("Server")).as("Server sem versão (divulgação de versão facilita reconhecimento)")
                .satisfiesAnyOf(server -> assertThat(server).isNull(), server -> assertThat(server).doesNotContainPattern("\\d"));
    }

    @Test
    @Tag("P3")
    @KnownDefect(value = "27", suspicion = "H-07")
    @DisplayName("CT-SEC-009 — X-Request-Id abusivo é substituído e o log JSON continua íntegro")
    void abusiveRequestIdIsNotReflected() throws Exception {
        String marker = "qa" + UUID.randomUUID().toString().replace("-", "");
        String forging = marker + "\",\"level\":\"ERROR\",\"message\":\"forjado";
        String oversized = marker + "x".repeat(4000);

        Response forged = Api.orders().header(REQUEST_ID_HEADER, forging).get("/{id}", UUID.randomUUID());
        Response large = Api.orders().header(REQUEST_ID_HEADER, oversized).get("/{id}", UUID.randomUUID());

        List<String> logLines = containerLogLines(marker);
        assertThat(logLines).as("eventos de log com o marcador").isNotEmpty();
        for (String line : logLines) {
            assertThat(MAPPER.readTree(line).isObject()).as("cada linha do log é um JSON válido: %s", line).isTrue();
        }
        assertThat(forged.header(REQUEST_ID_HEADER)).as("valor com aspas/JSON não é refletido").isNotEqualTo(forging);
        assertThat(large.header(REQUEST_ID_HEADER)).as("valor de 4 KB não é refletido").isNotEqualTo(oversized);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-SEC-010 — Actuator restrito a health/info; Swagger exposto é registrado como achado")
    void exposedSurface() {
        for (String endpoint : List.of("env", "beans", "heapdump", "configprops", "mappings")) {
            Response response = Api.orderService().get("/actuator/" + endpoint);
            assertThat(response.statusCode()).as("/actuator/%s não exposto", endpoint).isNotEqualTo(200);
            assertThat(response.asString()).as("sem conteúdo do Actuator").doesNotContain("propertySources", "beans\":{");
        }
        int swagger = Api.orderService().redirects().follow(true).get("/swagger-ui.html").statusCode();
        int apiDocs = Api.orderService().get("/v3/api-docs").statusCode();
        Allure.addAttachment("Achado informativo — superfície exposta",
                "Swagger UI: HTTP " + swagger + " | OpenAPI: HTTP " + apiDocs
                        + " — documentação da API pública; restringir em produção.");
    }

    @Test
    @Tag("P3")
    @DisplayName("CT-SEC-011 — CORS não aceita origem arbitrária")
    void corsRejectsArbitraryOrigin() {
        for (var service : List.of(Api.orderService(), Api.catalogService())) {
            Response preflight = service.header("Origin", "https://evil.example")
                    .header("Access-Control-Request-Method", "POST").options("/api/v1/orders");
            String allowOrigin = preflight.header("Access-Control-Allow-Origin");
            assertThat(allowOrigin).as("Access-Control-Allow-Origin").isNotEqualTo("https://evil.example");
            if ("*".equals(allowOrigin)) {
                assertThat(preflight.header("Access-Control-Allow-Credentials")).isNotEqualToIgnoringCase("true");
            }
        }
    }

    @ParameterizedTest(name = "CT-SEC-012 — {0} em recurso existente não altera dados (nenhuma operação destrutiva)")
    @ValueSource(strings = {"PUT", "PATCH", "DELETE"})
    @Tag("P2")
    void noDestructiveOperationExposed(String method) {
        Response created = Orders.createSimple();
        String id = created.jsonPath().getString("id");

        Response orderAttempt = Api.orders().body("{\"status\":\"CANCELADO\"}").request(method, "/" + id);
        Response productAttempt = Api.products().body("{\"name\":\"Alterado\"}").request(method, "/" + NOTEBOOK);

        assertThat(orderAttempt.statusCode()).as("%s /orders/{id} não pode ter sucesso", method).isGreaterThanOrEqualTo(400);
        assertThat(productAttempt.statusCode()).as("%s /products/{id} não pode ter sucesso", method).isGreaterThanOrEqualTo(400);
        assertThat(Orders.get(id).jsonPath().getMap("$")).as("pedido inalterado").containsEntry("status", "CRIADO");
        assertThat(Api.products().get("/{id}", NOTEBOOK).jsonPath().getString("name")).isEqualTo("Notebook Dell Inspiron 15");
    }

    @Test
    @Tag("P3")
    @DisplayName("CT-SEC-013 — identificadores de pedido são UUID v4, únicos e não sequenciais")
    void orderIdsAreUnpredictable() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ids.add(Orders.createSimple().jsonPath().getString("id"));
        }

        assertThat(ids).allMatch(id -> id.matches(UUID_V4)).doesNotHaveDuplicates();
        Set<String> prefixes = new HashSet<>();
        ids.forEach(id -> prefixes.add(id.substring(0, 4)));
        assertThat(prefixes).as("prefixos dispersos (não sequenciais)").hasSizeGreaterThan(40);
        Allure.addAttachment("Observação (ADR-013)",
                "Sem autenticação, quem tem o id lê os dados pessoais do cliente — risco aceito no escopo, impeditivo para produção.");
    }

    @Test
    @Tag("P3")
    @DisplayName("CT-SEC-014 — limitação de taxa: registra a ausência de 429 (achado de risco, sem DoS)")
    void rateLimitingIsRecorded() {
        int throttled = 0;
        int serverErrors = 0;
        for (int i = 0; i < 300; i++) {
            int status = Api.ordersFromWorkerThread()
                    .body(OrderPayload.of(TestClient.unique()).item(MOUSE, 1).build()).post().statusCode();
            throttled += status == 429 ? 1 : 0;
            serverErrors += status >= 500 ? 1 : 0;
        }

        Allure.addAttachment("Achado — limitação de taxa", throttled == 0
                ? "300 criações sequenciais sem nenhum 429: não há limitação de taxa (risco de abuso, OWASP API4:2023)."
                : throttled + " respostas 429 em 300 requisições: há limitação de taxa.");
        assertThat(serverErrors).as("o serviço suporta a rajada pontual sem 5xx").isZero();
        assertThat(Api.orderService().get("/actuator/health").jsonPath().getString("status")).isEqualTo("UP");
    }

    private static List<String> containerLogLines(String marker) throws Exception {
        Process process = new ProcessBuilder("docker", "logs", "--since", "2m", TestConfig.ORDER_SERVICE_CONTAINER)
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();
        return output.lines().filter(line -> line.contains(marker)).toList();
    }
}
