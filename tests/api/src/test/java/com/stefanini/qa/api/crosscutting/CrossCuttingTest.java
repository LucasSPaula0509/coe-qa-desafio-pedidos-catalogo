package com.stefanini.qa.api.crosscutting;

import static com.stefanini.qa.api.data.Products.HEADSET_EXPIRED_PRICE;
import static com.stefanini.qa.api.data.Products.MISSING;
import static com.stefanini.qa.api.data.Products.MONITOR_INACTIVE;
import static com.stefanini.qa.api.data.Products.MOUSE;
import static com.stefanini.qa.api.support.ApiAssertions.REQUEST_ID_HEADER;
import static com.stefanini.qa.api.support.ApiAssertions.assertClientError;
import static com.stefanini.qa.api.support.ApiAssertions.assertError;
import static com.stefanini.qa.api.support.ApiAssertions.assertNoImplementationLeak;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.data.OrderPayload;
import com.stefanini.qa.api.data.TestClient;
import com.stefanini.qa.api.fault.CatalogProxy;
import com.stefanini.qa.api.support.Api;
import com.stefanini.qa.api.support.Orders;
import com.stefanini.qa.api.support.KnownDefect;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** docs/test-cases/api-cross-cutting.md — regras transversais dos dois serviços. */
@Tag("api")
@DisplayName("Transversais — envelope de erro, X-Request-Id, semântica HTTP, saúde")
class CrossCuttingTest {

    private static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @AfterEach
    void restoreProxy() {
        CatalogProxy.reset();
    }

    /** Um disparador por erro documentado (RN-23); 503/504 via proxy de falhas. */
    static Stream<Arguments> documentedErrors() {
        return Stream.of(
                error("order 400 validação", 400, "VALIDATION_ERROR",
                        () -> Orders.create(OrderPayload.of(TestClient.unique()).item(MOUSE, 1).item(MOUSE, 1).build())),
                error("order 400 JSON malformado", 400, "VALIDATION_ERROR", () -> Api.orders().body("{").post()),
                error("order 404", 404, "ORDER_NOT_FOUND", () -> Orders.get(UUID.randomUUID().toString())),
                error("order 413", 413, "PAYLOAD_TOO_LARGE",
                        () -> Api.orders().body(" ".repeat(1_048_577)).post()),
                error("order 422 inexistente", 422, "PRODUCT_NOT_FOUND", () -> createWith(MISSING)),
                error("order 422 inativo", 422, "PRODUCT_INACTIVE", () -> createWith(MONITOR_INACTIVE)),
                error("order 422 sem preço", 422, "NO_CURRENT_PRICE", () -> createWith(HEADSET_EXPIRED_PRICE)),
                error("order 503", 503, "CATALOG_UNAVAILABLE", () -> {
                    CatalogProxy.failPricesWithConnectionReset();
                    return createWith(MOUSE);
                }),
                error("order 504", 504, "CATALOG_TIMEOUT", () -> {
                    CatalogProxy.delayPrices(5_000);
                    return createWith(MOUSE);
                }),
                error("catalog 400 validação", 400, "VALIDATION_ERROR",
                        () -> Api.products().body(Map.of("productIds", List.of())).post("/prices")),
                error("catalog 400 JSON malformado", 400, "VALIDATION_ERROR",
                        () -> Api.products().body("{").post("/prices")),
                error("catalog 404", 404, "PRODUCT_NOT_FOUND", () -> Api.products().get("/{id}", MISSING)));
    }

    @ParameterizedTest(name = "CT-TRV-001 — envelope de erro: {0}")
    @MethodSource("documentedErrors")
    @Tag("P1")
    @Tag("fault")
    @KnownDefect("H-11")
    void documentedErrorsUseTheEnvelope(String trigger, int status, String code, Supplier<Response> call) {
        assertError(call.get(), status, code);
    }

    static Stream<Arguments> unexpectedErrorTriggers() {
        return Stream.of(
                Arguments.of("id malformado", (Supplier<Response>) () -> Orders.get("abc")),
                Arguments.of("page não numérica", (Supplier<Response>) () -> Api.orders().queryParam("page", "abc").get()),
                Arguments.of("Content-Type não suportado",
                        (Supplier<Response>) () -> Api.orders().contentType("text/plain").body("x").post()),
                Arguments.of("JSON malformado", (Supplier<Response>) () -> Api.orders().body("{\"items\": [1,").post()),
                Arguments.of("tipo inválido no lote",
                        (Supplier<Response>) () -> Api.products().body("{\"productIds\": [{}]}").post("/prices")));
    }

    @ParameterizedTest(name = "CT-TRV-002 — erro não vaza detalhes de implementação: {0}")
    @MethodSource("unexpectedErrorTriggers")
    @Tag("P1")
    void errorsDoNotLeakImplementationDetails(String trigger, Supplier<Response> call) {
        Response response = call.get();

        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
        assertNoImplementationLeak(response);
    }

    static Stream<Arguments> services() {
        return Stream.of(
                Arguments.of("order-service", (Supplier<RequestSpecification>) Api::orderService,
                        Api.ORDERS_PATH + "/" + UUID.randomUUID()),
                Arguments.of("catalog-service", (Supplier<RequestSpecification>) Api::catalogService,
                        Api.PRODUCTS_PATH + "/" + MISSING));
    }

    @ParameterizedTest(name = "CT-TRV-003 — X-Request-Id gerado quando ausente: {0}")
    @MethodSource("services")
    @Tag("P1")
    void requestIdIsGeneratedWhenAbsent(String service, Supplier<RequestSpecification> api, String notFoundPath) {
        Response success = api.get().get("/actuator/health");
        Response error = api.get().get(notFoundPath);
        Response another = api.get().get("/actuator/health");

        assertThat(success.header(REQUEST_ID_HEADER)).as("sucesso").matches(UUID_PATTERN);
        assertThat(error.header(REQUEST_ID_HEADER)).as("erro").matches(UUID_PATTERN);
        assertThat(error.jsonPath().getString("requestId")).isEqualTo(error.header(REQUEST_ID_HEADER));
        assertThat(another.header(REQUEST_ID_HEADER)).as("valores distintos por requisição")
                .isNotEqualTo(success.header(REQUEST_ID_HEADER));
    }

    @ParameterizedTest(name = "CT-TRV-004 — X-Request-Id do cliente é ecoado: {0}")
    @MethodSource("services")
    @Tag("P1")
    void clientRequestIdIsEchoed(String service, Supplier<RequestSpecification> api, String notFoundPath) {
        String requestId = "trv-" + UUID.randomUUID();

        Response success = api.get().header(REQUEST_ID_HEADER, requestId).get("/actuator/health");
        Response error = api.get().header(REQUEST_ID_HEADER, requestId).get(notFoundPath);

        assertThat(success.header(REQUEST_ID_HEADER)).isEqualTo(requestId);
        assertThat(error.header(REQUEST_ID_HEADER)).isEqualTo(requestId);
        assertThat(error.jsonPath().getString("requestId")).isEqualTo(requestId);
    }

    @ParameterizedTest(name = "CT-TRV-005 — X-Request-Id em branco é substituído: {0}")
    @MethodSource("services")
    @Tag("P3")
    void blankRequestIdIsReplaced(String service, Supplier<RequestSpecification> api, String notFoundPath) {
        Response response = api.get().header(REQUEST_ID_HEADER, "   ").get("/actuator/health");

        assertThat(response.header(REQUEST_ID_HEADER)).isNotBlank();
    }

    static Stream<Arguments> unknownRoutes() {
        return Stream.of(
                Arguments.of("order-service", (Supplier<RequestSpecification>) Api::orderService, "/api/v1/unknown"),
                Arguments.of("order-service", (Supplier<RequestSpecification>) Api::orderService, "/api/v2/orders"),
                Arguments.of("catalog-service", (Supplier<RequestSpecification>) Api::catalogService, "/api/v1/products"));
    }

    @ParameterizedTest(name = "CT-TRV-006 — rota inexistente {0} {2} → 404 com envelope")
    @MethodSource("unknownRoutes")
    @Tag("P2")
    @KnownDefect("H-02")
    void unknownRouteReturns404(String service, Supplier<RequestSpecification> api, String path) {
        assertClientError(api.get().get(path), 404);
    }

    static Stream<Arguments> unsupportedMethods() {
        return Stream.of(
                Arguments.of("PUT", false), Arguments.of("PATCH", false), Arguments.of("DELETE", false),
                Arguments.of("PUT", true), Arguments.of("PATCH", true), Arguments.of("DELETE", true));
    }

    @ParameterizedTest(name = "CT-TRV-007 — método {0} não suportado (no recurso /orders/<id>: {1}) → 405 com envelope")
    @MethodSource("unsupportedMethods")
    @Tag("P2")
    @KnownDefect("H-02")
    void unsupportedMethodsAreRejected(String method, boolean onResource) {
        Response created = Orders.createSimple();
        String id = created.jsonPath().getString("id");

        Response response = Api.orders().body("{}").request(method, onResource ? "/" + id : "");

        assertClientError(response, 405);
        Response after = Orders.get(id);
        assertThat(after.statusCode()).as("pedido continua existindo").isEqualTo(200);
        assertThat(after.jsonPath().getMap("items[0]")).isEqualTo(created.jsonPath().getMap("items[0]"));
    }

    @ParameterizedTest(name = "CT-TRV-008 — saúde do serviço: {0}")
    @MethodSource("services")
    @Tag("P1")
    void healthIsUp(String service, Supplier<RequestSpecification> api, String notFoundPath) {
        Response response = api.get().get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("status")).isEqualTo("UP");
    }

    static Stream<Arguments> hiddenActuatorEndpoints() {
        return services().flatMap(service -> Stream.of("env", "beans", "heapdump", "configprops")
                .map(endpoint -> Arguments.of(service.get()[0], service.get()[1], "/actuator/" + endpoint)));
    }

    @ParameterizedTest(name = "CT-TRV-009 — Actuator não expõe {2}: {0}")
    @MethodSource("hiddenActuatorEndpoints")
    @Tag("P2")
    @KnownDefect("H-02")
    void actuatorExposesOnlyHealthAndInfo(String service, Supplier<RequestSpecification> api, String path) {
        Response response = api.get().get(path);

        assertThat(response.statusCode()).as("%s não exposto — corpo: %s", path, response.asString()).isEqualTo(404);
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-TRV-010 — respostas de sucesso, erro e 413 são application/json")
    void responsesAreJson() {
        List<Response> responses = List.of(
                Orders.createSimple(),
                Orders.get(UUID.randomUUID().toString()),
                Api.orders().body(" ".repeat(1_048_577)).post(),
                Api.products().get("/{id}", MOUSE),
                Api.products().get("/{id}", MISSING));

        assertThat(responses).allSatisfy(response ->
                assertThat(response.contentType()).as("status %d", response.statusCode()).startsWith("application/json"));
    }

    @Test
    @Tag("P3")
    @DisplayName("CT-TRV-011 — OpenAPI publicado contém os contratos documentados")
    void openApiMatchesDocumentedContracts() {
        Map<String, Object> orderPaths = Api.orderService().get("/v3/api-docs").then().statusCode(200)
                .extract().jsonPath().getMap("paths");
        Map<String, Object> catalogPaths = Api.catalogService().get("/v3/api-docs").then().statusCode(200)
                .extract().jsonPath().getMap("paths");

        assertThat(operations(orderPaths, "/api/v1/orders")).contains("get", "post");
        assertThat(operations(orderPaths, "/api/v1/orders/{id}")).contains("get");
        assertThat(operations(catalogPaths, "/api/v1/products/{id}")).contains("get");
        assertThat(operations(catalogPaths, "/api/v1/products/prices")).contains("post");
    }

    @SuppressWarnings("unchecked")
    private static Set<String> operations(Map<String, Object> paths, String path) {
        assertThat(paths).as("caminho documentado no OpenAPI").containsKey(path);
        return ((Map<String, Object>) paths.get(path)).keySet();
    }

    private static Response createWith(String productId) {
        return Orders.create(OrderPayload.of(TestClient.unique()).item(productId, 1).build());
    }

    private static Arguments error(String trigger, int status, String code, Supplier<Response> call) {
        return Arguments.of(trigger, status, code, call);
    }
}
