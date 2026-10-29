package com.stefanini.qa.api.fault;

import static io.restassured.RestAssured.given;

import com.stefanini.qa.api.support.Api;
import com.stefanini.qa.api.support.TestConfig;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Controle do proxy WireMock posicionado entre order-service e catalog-service
 * (tests/infra/docker-compose.qa.yml). Sem stubs, o proxy é transparente. Os stubs criados aqui
 * têm prioridade sobre o proxy padrão e são removidos por {@link #reset()}.
 */
public final class CatalogProxy {

    public static final String PRICES_PATH = "/api/v1/products/prices";
    private static final String CATALOG_UPSTREAM = "http://catalog-service:8081";

    private CatalogProxy() {
    }

    /**
     * Remove apenas os stubs criados pelos testes (marcados com metadata "qa") e limpa o registro
     * de requisições. O mapping padrão de proxy (--proxy-all) é preservado.
     */
    public static void reset() {
        admin().contentType(ContentType.JSON)
                .body(Map.of("matchesJsonPath", Map.of("expression", "$.qa", "equalTo", "fault")))
                .post("/__admin/mappings/remove-by-metadata").then().statusCode(200);
        resetRequests();
    }

    public static void resetRequests() {
        admin().delete("/__admin/requests").then().statusCode(200);
    }

    /** Encaminha ao catálogo real, mas com atraso fixo (simula catálogo lento). */
    public static void delayPrices(int milliseconds) {
        stubPrices(Map.of("proxyBaseUrl", CATALOG_UPSTREAM, "fixedDelayMilliseconds", milliseconds));
    }

    /** Derruba a conexão (simula catálogo inacessível: erro de IO na chamada). */
    public static void failPricesWithConnectionReset() {
        stubPrices(Map.of("fault", "CONNECTION_RESET_BY_PEER"));
    }

    /** Responde com o status informado (ex.: 500 do catálogo). */
    public static void respondPricesWithStatus(int status) {
        stubPrices(Map.of("status", status, "headers", Map.of("Content-Type", "application/json"),
                "body", "{\"error\":{\"code\":\"INTERNAL_ERROR\",\"message\":\"injected\",\"details\":[]}}"));
    }

    /** Para o container do proxy: o order-service passa a ter o destino do catálogo inacessível. */
    public static void stopContainer() {
        docker("stop", TestConfig.CATALOG_PROXY_CONTAINER);
    }

    /**
     * Religa o container do proxy e aguarda até que o order-service volte a alcançá-lo. O container
     * religado pode receber outro IP, e a JVM do order-service mantém o DNS em cache por alguns
     * segundos; por isso a prontidão é medida pelo caminho real do pedido, não só pelo proxy.
     *
     * @return tempo até o order-service voltar a consultar o catálogo
     */
    public static Duration startContainer() {
        long start = System.nanoTime();
        docker("start", TestConfig.CATALOG_PROXY_CONTAINER);
        awaitReady();
        awaitOrderServiceReachesCatalog();
        return Duration.ofNanos(System.nanoTime() - start);
    }

    /**
     * Aguarda (até 90 s) o order-service voltar a consultar o catálogo. Usa como sonda um pedido com
     * produto inexistente: exige a chamada ao catálogo e resulta em 422, sem persistir nada.
     */
    public static void awaitOrderServiceReachesCatalog() {
        Map<String, Object> probe = Map.of(
                "client", Map.of("name", "Sonda QA", "document", "0", "email", "sonda.qa@example.com"),
                "items", List.of(Map.of("productId", "QA-PROBE-NOT-FOUND", "quantity", 1)));
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                if (Api.ordersFromWorkerThread().body(probe).post().statusCode() == 422) {
                    return;
                }
            } catch (Exception notReadyYet) {
                // order-service ainda sem rota até o proxy
            }
            LockSupport.parkNanos(Duration.ofSeconds(1).toNanos());
        }
        throw new IllegalStateException("order-service não voltou a alcançar o catálogo em 90 s");
    }

    /**
     * Aguarda (até 60 s) a API administrativa responder com o mapping de proxy padrão ativo. Usado
     * após religar o container e como verificação de isolamento entre casos de injeção de falha.
     */
    public static void awaitReady() {
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                String mappings = admin().get("/__admin/mappings").then().statusCode(200).extract().asString();
                if (mappings.contains(CATALOG_UPSTREAM)) {
                    return;
                }
            } catch (Exception notReadyYet) {
                // conexão recusada ou sem resposta enquanto o WireMock sobe
            }
            LockSupport.parkNanos(Duration.ofMillis(500).toNanos());
        }
        throw new IllegalStateException("Proxy do catálogo não ficou pronto em 60 s");
    }

    private static void docker(String... args) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.addAll(List.of(args));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            if (!process.waitFor(90, TimeUnit.SECONDS) || process.exitValue() != 0) {
                throw new IllegalStateException("Falha em " + command + ": " + output);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Docker CLI indisponível para " + command, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido em " + command, e);
        }
    }

    /** Requisições recebidas pelo proxy desde o último reset, da mais recente para a mais antiga. */
    public static List<Map<String, Object>> receivedRequests() {
        return admin().get("/__admin/requests").then().statusCode(200).extract().jsonPath()
                .getList("requests.request");
    }

    private static void stubPrices(Map<String, Object> response) {
        Map<String, Object> mapping = Map.of(
                "priority", 1,
                "request", Map.of("method", "POST", "urlPath", PRICES_PATH),
                "response", response,
                "metadata", Map.of("qa", "fault"));
        admin().contentType(ContentType.JSON).body(mapping).post("/__admin/mappings").then().statusCode(201);
    }

    private static io.restassured.specification.RequestSpecification admin() {
        return given().baseUri(TestConfig.CATALOG_PROXY_ADMIN_URL);
    }
}
