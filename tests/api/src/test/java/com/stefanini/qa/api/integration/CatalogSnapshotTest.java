package com.stefanini.qa.api.integration;

import static com.stefanini.qa.api.data.Products.MOUSE;
import static com.stefanini.qa.api.data.Products.MOUSE_NAME;
import static com.stefanini.qa.api.data.Products.MOUSE_PRICE;
import static com.stefanini.qa.api.support.ApiAssertions.assertError;
import static com.stefanini.qa.api.support.Money.assertAmount;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.data.OrderPayload;
import com.stefanini.qa.api.data.TestClient;
import com.stefanini.qa.api.db.CatalogDatabase;
import com.stefanini.qa.api.support.Api;
import com.stefanini.qa.api.support.Orders;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.Date;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * docs/test-cases/integration.md — snapshot de preço e nome (ADR-006 do SUT). Altera o produto
 * PROD-002 diretamente no MongoDB (o catálogo não tem escrita) e restaura o documento ao final.
 */
@Tag("api")
@Tag("integration")
@Tag("db")
@Tag("serial")
@DisplayName("Integração — snapshot de preço e nome")
class CatalogSnapshotTest {

    private static final String NEW_NAME = "Mouse Logitech MX Master 3S (nova versão)";
    private static final String NEW_PRICE = "599.90";

    private Document snapshot;

    @BeforeEach
    void snapshotProduct() {
        snapshot = CatalogDatabase.snapshot(MOUSE);
    }

    @AfterEach
    void restoreProduct() {
        CatalogDatabase.restore(snapshot);
        assertThat(CatalogDatabase.snapshot(MOUSE)).as("produto restaurado").isEqualTo(snapshot);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-INT-002 — mudança de preço e nome no catálogo não altera pedidos existentes")
    void repricingDoesNotAffectExistingOrders() {
        String oldOrderId = createOrderWithMouse().jsonPath().getString("id");

        CatalogDatabase.rename(MOUSE, NEW_NAME);
        CatalogDatabase.reprice(MOUSE, new BigDecimal(NEW_PRICE), new Date());

        JsonPath oldOrder = Orders.get(oldOrderId).then().statusCode(200).extract().jsonPath();
        assertThat(oldOrder.getString("items[0].productName")).as("nome registrado no pedido antigo").isEqualTo(MOUSE_NAME);
        assertAmount(oldOrder.get("items[0].unitPrice"), MOUSE_PRICE, "preço registrado no pedido antigo");
        JsonPath listed = Api.orders().get().then().statusCode(200).extract().jsonPath();
        String listedItem = "content.find { it.id == '" + oldOrderId + "' }.items[0]";
        assertAmount(listed.get(listedItem + ".unitPrice"), MOUSE_PRICE, "preço do pedido antigo na listagem");

        JsonPath newOrder = createOrderWithMouse().jsonPath();
        assertThat(newOrder.getString("items[0].productName")).as("novo pedido usa o nome atual").isEqualTo(NEW_NAME);
        assertAmount(newOrder.get("items[0].unitPrice"), NEW_PRICE, "novo pedido usa o preço vigente");
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-INT-003 — produto inativado passa a ser recusado sem afetar pedidos antigos")
    void deactivatedProductIsRejectedForNewOrders() {
        Response oldOrder = createOrderWithMouse();

        CatalogDatabase.setStatus(MOUSE, "INATIVO");

        assertError(Orders.create(OrderPayload.of(TestClient.unique()).item(MOUSE, 1).build()), 422, "PRODUCT_INACTIVE");
        JsonPath created = oldOrder.jsonPath();
        JsonPath fetched = Orders.get(created.getString("id")).then().statusCode(200).extract().jsonPath();
        for (String field : new String[] {"client", "items", "subtotal", "discount", "total", "status"}) {
            assertThat((Object) fetched.get(field)).as("pedido antigo inalterado: %s", field)
                    .isEqualTo(created.get(field));
        }
    }

    private static Response createOrderWithMouse() {
        return Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(MOUSE, 1));
    }
}
