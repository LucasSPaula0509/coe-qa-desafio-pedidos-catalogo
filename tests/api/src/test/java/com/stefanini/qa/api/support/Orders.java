package com.stefanini.qa.api.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.data.OrderPayload;
import com.stefanini.qa.api.data.TestClient;
import io.restassured.response.Response;

/** Operações de pedido usadas como passo de teste ou de preparação. */
public final class Orders {

    private Orders() {
    }

    public static Response create(Object body) {
        return Api.orders().body(body).post();
    }

    /** Cria um pedido que deve ser aceito (preparação); falha com o corpo da resposta se não for. */
    public static Response createAccepted(OrderPayload payload) {
        Response response = create(payload.build());
        assertThat(response.statusCode()).as("pré-condição: pedido criado — corpo: %s", response.asString())
                .isEqualTo(201);
        return response;
    }

    /** Pedido simples e válido (PROD-002 x 1) para um cliente novo. */
    public static Response createSimple() {
        return createAccepted(OrderPayload.of(TestClient.unique()).item("PROD-002", 1));
    }

    public static Response get(String id) {
        return Api.orders().get("/{id}", id);
    }
}
