package com.stefanini.qa.api.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construtor do corpo de POST /api/v1/orders. Usa mapas (e não DTOs tipados) para permitir
 * payloads inválidos: campos ausentes ({@link #ABSENT}), nulos, tipos errados e campos extras.
 */
public final class OrderPayload {

    /** Marca um campo para ser omitido do JSON (diferente de enviá-lo com null). */
    public static final Object ABSENT = new Object();

    private final Map<String, Object> root = new LinkedHashMap<>();
    private final Map<String, Object> client = new LinkedHashMap<>();
    private final List<Map<String, Object>> items = new ArrayList<>();

    private OrderPayload(TestClient testClient) {
        client.put("name", testClient.name());
        client.put("document", testClient.document());
        client.put("email", testClient.email());
    }

    public static OrderPayload of(TestClient client) {
        return new OrderPayload(client);
    }

    public OrderPayload item(String productId, Object quantity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("productId", productId);
        item.put("quantity", quantity);
        items.add(item);
        return this;
    }

    /** Item com campos arbitrários (ex.: preço forjado pelo cliente). */
    public OrderPayload item(Map<String, Object> rawItem) {
        items.add(new LinkedHashMap<>(rawItem));
        return this;
    }

    public OrderPayload clientField(String field, Object value) {
        client.put(field, value);
        return this;
    }

    public OrderPayload rootField(String field, Object value) {
        root.put(field, value);
        return this;
    }

    public Map<String, Object> build() {
        Map<String, Object> body = new LinkedHashMap<>();
        if (!root.containsKey("client")) {
            body.put("client", withoutAbsent(client));
        }
        if (!root.containsKey("items")) {
            body.put("items", items.stream().map(OrderPayload::withoutAbsent).toList());
        }
        root.forEach((key, value) -> {
            if (value != ABSENT) {
                body.put(key, value);
            }
        });
        return body;
    }

    private static Map<String, Object> withoutAbsent(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (value != ABSENT) {
                copy.put(key, value);
            }
        });
        return copy;
    }
}
