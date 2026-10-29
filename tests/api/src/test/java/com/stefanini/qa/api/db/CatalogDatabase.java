package com.stefanini.qa.api.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.stefanini.qa.api.support.TestConfig;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.Decimal128;

/**
 * Acesso direto ao MongoDB do catalog-service. O catálogo não tem endpoints de escrita (RN-21),
 * então mudanças de preço/situação para testes de integração são feitas aqui e sempre revertidas
 * a partir de um snapshot do documento.
 */
public final class CatalogDatabase {

    private static final MongoClient CLIENT = MongoClients.create(TestConfig.MONGODB_URI);

    private CatalogDatabase() {
    }

    public static Document snapshot(String productId) {
        Document product = products().find(Filters.eq("_id", productId)).first();
        if (product == null) {
            throw new IllegalStateException("Produto da massa inicial ausente: " + productId);
        }
        return product;
    }

    public static void restore(Document snapshot) {
        products().replaceOne(Filters.eq("_id", snapshot.get("_id")), snapshot);
    }

    public static void setStatus(String productId, String status) {
        products().updateOne(Filters.eq("_id", productId), Updates.set("status", status));
    }

    public static void rename(String productId, String name) {
        products().updateOne(Filters.eq("_id", productId), Updates.set("name", name));
    }

    /**
     * Encerra todas as janelas vigentes no instante {@code switchAt} e abre uma nova janela com o
     * novo valor a partir dele (reajuste de preço, ADR-007).
     */
    public static void reprice(String productId, BigDecimal newValue, Date switchAt) {
        Document product = snapshot(productId);
        List<Document> prices = product.getList("prices", Document.class);
        for (Document price : prices) {
            if (price.get("validTo") == null) {
                price.put("validTo", switchAt);
            }
        }
        prices.add(new Document("value", new Decimal128(newValue))
                .append("currency", "BRL")
                .append("validFrom", switchAt)
                .append("validTo", null));
        products().updateOne(Filters.eq("_id", productId), Updates.set("prices", prices));
    }

    private static MongoCollection<Document> products() {
        return CLIENT.getDatabase(TestConfig.MONGODB_DATABASE).getCollection("products");
    }
}
