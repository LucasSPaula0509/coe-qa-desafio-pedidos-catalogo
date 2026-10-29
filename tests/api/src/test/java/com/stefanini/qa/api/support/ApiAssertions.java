package com.stefanini.qa.api.support;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.db.OrdersDatabase;
import io.restassured.response.Response;
import java.util.List;

/** Asserções reutilizáveis derivadas das regras transversais (RN-23, RN-24, RN-25, RN-07). */
public final class ApiAssertions {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** Fragmentos que não podem aparecer em respostas de erro (RN-23: sem detalhes de implementação). */
    private static final List<String> LEAK_MARKERS = List.of(
            "com.stefanini", "java.", "javax.", "jakarta.", "org.springframework", "org.hibernate",
            "Exception", "at com.", "SELECT ", "INSERT ", "SQLState", "/app/", ".java:");

    private ApiAssertions() {
    }

    /** Erro com status e código exatos, envelope válido e requestId coerente com o header. */
    public static void assertError(Response response, int status, String code) {
        assertThat(response.statusCode()).as("status HTTP — corpo: %s", response.asString()).isEqualTo(status);
        assertErrorEnvelope(response);
        assertThat(response.jsonPath().getString("error.code")).as("error.code").isEqualTo(code);
    }

    /**
     * Erro de entrada do cliente: status 4xx entre os permitidos, com envelope. Usado quando a
     * documentação não fixa o status exato, mas proíbe 5xx para entrada inválida (RN-25).
     */
    public static void assertClientError(Response response, Integer... allowedStatuses) {
        assertThat(response.statusCode())
                .as("entrada inválida deve resultar em 4xx documentado, nunca 5xx (RN-25) — corpo: %s",
                        response.asString())
                .isIn((Object[]) allowedStatuses);
        assertErrorEnvelope(response);
    }

    /** Envelope de erro conforme ADR-008 e X-Request-Id coerente (RN-23, RN-24). */
    public static void assertErrorEnvelope(Response response) {
        assertThat(response.contentType()).as("erro deve ser JSON com envelope (RN-23) — corpo: %s",
                abbreviate(response.asString())).startsWith("application/json");
        response.then().body(matchesJsonSchemaInClasspath("schemas/error-envelope.json"));
        String header = response.header(REQUEST_ID_HEADER);
        assertThat(header).as("header X-Request-Id").isNotBlank();
        assertThat(response.jsonPath().getString("requestId")).as("requestId do envelope = header").isEqualTo(header);
        assertNoImplementationLeak(response);
    }

    public static void assertNoImplementationLeak(Response response) {
        String body = response.asString();
        assertThat(LEAK_MARKERS).as("corpo de erro não deve expor detalhes de implementação: %s", body)
                .noneMatch(body::contains);
    }

    private static String abbreviate(String body) {
        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }

    public static void assertDetailsContain(Response response, String fragment) {
        List<String> details = response.jsonPath().getList("error.details", String.class);
        assertThat(details).as("error.details").anyMatch(detail -> detail.contains(fragment));
    }

    public static void assertDetailsExactly(Response response, String... values) {
        assertThat(response.jsonPath().getList("error.details", String.class)).as("error.details")
                .containsExactlyInAnyOrder(values);
    }

    /** All-or-nothing (RN-07): nenhum pedido nem item gravado para o cliente. */
    public static void assertNothingPersisted(String clientEmail) {
        assertThat(OrdersDatabase.countOrders(clientEmail)).as("pedidos persistidos para %s", clientEmail).isZero();
        assertThat(OrdersDatabase.countItems(clientEmail)).as("itens persistidos para %s", clientEmail).isZero();
    }

    public static void assertMatchesSchema(Response response, String schema) {
        response.then().body(matchesJsonSchemaInClasspath("schemas/" + schema));
    }
}
