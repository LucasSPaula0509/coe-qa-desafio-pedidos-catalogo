package com.stefanini.qa.api.support;

import static io.restassured.RestAssured.given;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.specification.RequestSpecification;

/**
 * Ponto único de acesso HTTP aos serviços. Toda requisição é anexada ao relatório Allure
 * (requisição + resposta) e os números do JSON são lidos como BigDecimal, para comparar
 * valores monetários sem erro de ponto flutuante (ADR-015 do SUT).
 */
public final class Api {

    public static final String ORDERS_PATH = "/api/v1/orders";
    public static final String PRODUCTS_PATH = "/api/v1/products";

    private static final RestAssuredConfig CONFIG = RestAssuredConfig.config()
            .jsonConfig(JsonConfig.jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL));

    private Api() {
    }

    /** order-service com base em /api/v1/orders. */
    public static RequestSpecification orders() {
        return service(TestConfig.ORDER_BASE_URL).basePath(ORDERS_PATH);
    }

    /**
     * order-service sem o filtro do Allure, para requisições disparadas em threads auxiliares
     * (o Allure só anexa evidências na thread do teste).
     */
    public static RequestSpecification ordersFromWorkerThread() {
        return given().baseUri(TestConfig.ORDER_BASE_URL).basePath(ORDERS_PATH)
                .contentType(ContentType.JSON).accept(ContentType.JSON).config(CONFIG);
    }

    /** catalog-service com base em /api/v1/products. */
    public static RequestSpecification products() {
        return service(TestConfig.CATALOG_BASE_URL).basePath(PRODUCTS_PATH);
    }

    /** order-service na raiz (actuator, OpenAPI, rotas arbitrárias). */
    public static RequestSpecification orderService() {
        return service(TestConfig.ORDER_BASE_URL);
    }

    /** catalog-service na raiz (actuator, OpenAPI, rotas arbitrárias). */
    public static RequestSpecification catalogService() {
        return service(TestConfig.CATALOG_BASE_URL);
    }

    private static RequestSpecification service(String baseUri) {
        return given().spec(new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(CONFIG)
                .addFilter(new AllureRestAssured())
                .build());
    }
}
