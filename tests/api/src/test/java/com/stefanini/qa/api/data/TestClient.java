package com.stefanini.qa.api.data;

import com.stefanini.qa.api.support.Ids;

/**
 * Cliente sintético e único por teste (CLIENTE_PADRAO em docs/test-cases/README.md). O e-mail
 * único permite verificar persistência diretamente no banco sem depender de outros testes.
 */
public record TestClient(String name, String document, String email) {

    public static final String DOCUMENT = "12345678901";

    public static TestClient unique() {
        String runId = Ids.runId();
        return new TestClient("Cliente QA " + runId, DOCUMENT, "qa." + runId + "@example.com");
    }
}
