package com.stefanini.qa.api.orders;

import static com.stefanini.qa.api.data.Products.KEYBOARD;
import static com.stefanini.qa.api.data.Products.KEYBOARD_PRICE;
import static com.stefanini.qa.api.data.Products.MOUSE;
import static com.stefanini.qa.api.data.Products.MOUSE_NAME;
import static com.stefanini.qa.api.data.Products.MOUSE_PRICE;
import static com.stefanini.qa.api.data.Products.NOTEBOOK;
import static com.stefanini.qa.api.data.Products.NOTEBOOK_NAME;
import static com.stefanini.qa.api.data.Products.NOTEBOOK_PRICE;
import static com.stefanini.qa.api.support.Money.assertAmount;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.data.OrderPayload;
import com.stefanini.qa.api.data.TestClient;
import com.stefanini.qa.api.support.ApiAssertions;
import com.stefanini.qa.api.support.Orders;
import com.stefanini.qa.api.support.KnownDefect;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** docs/test-cases/api-orders.md — A. Criação e cálculo (regra de desconto inicial: 1000,00 / 10%). */
@Tag("api")
@DisplayName("Pedidos — criação e cálculo")
class CreateOrderCalculationTest {

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-001 — pedido de 1 item abaixo do mínimo não recebe desconto")
    void singleItemBelowMinimum() {
        JsonPath order = Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(MOUSE, 1)).jsonPath();

        assertThat(order.getString("items[0].productName")).isEqualTo(MOUSE_NAME);
        assertAmount(order.get("items[0].unitPrice"), MOUSE_PRICE, "unitPrice");
        assertAmount(order.get("items[0].lineTotal"), "499.90", "lineTotal");
        assertTotals(order, "499.90", "0.00", "499.90");
        assertThat(order.getString("status")).isEqualTo("CRIADO");
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-002 — pedido com vários itens e desconto (exemplo oficial da documentação)")
    void documentedExampleWithDiscount() {
        JsonPath order = Orders.createAccepted(OrderPayload.of(TestClient.unique())
                .item(NOTEBOOK, 2).item(MOUSE, 1)).jsonPath();

        assertAmount(order.get("items.find { it.productId == 'PROD-001' }.lineTotal"), "7000.00", "lineTotal PROD-001");
        assertAmount(order.get("items.find { it.productId == 'PROD-002' }.lineTotal"), "499.90", "lineTotal PROD-002");
        assertTotals(order, "7499.90", "749.99", "6749.91");
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-003 — maior subtotal possível abaixo do mínimo (999,80) não recebe desconto")
    void highestSubtotalBelowMinimum() {
        JsonPath order = Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(MOUSE, 2)).jsonPath();

        assertTotals(order, "999.80", "0.00", "999.80");
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-004 — menor subtotal possível acima do mínimo (1149,80) recebe 10%")
    void lowestSubtotalAboveMinimum() {
        JsonPath order = Orders.createAccepted(OrderPayload.of(TestClient.unique())
                .item(MOUSE, 1).item(KEYBOARD, 1)).jsonPath();

        assertTotals(order, "1149.80", "114.98", "1034.82");
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-012 — quantidade máxima (999) em um item")
    void maximumQuantityInOneItem() {
        JsonPath order = Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(NOTEBOOK, 999)).jsonPath();

        assertAmount(order.get("items[0].lineTotal"), "3496500.00", "lineTotal");
        assertTotals(order, "3496500.00", "349650.00", "3146850.00");
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-013 — maior valor agregado com a massa (3 produtos válidos x 999)")
    void largestAggregateAmount() {
        JsonPath order = Orders.createAccepted(OrderPayload.of(TestClient.unique())
                .item(NOTEBOOK, 999).item(MOUSE, 999).item(KEYBOARD, 999)).jsonPath();

        assertAmount(order.get("items.find { it.productId == 'PROD-001' }.lineTotal"), "3496500.00", "lineTotal PROD-001");
        assertAmount(order.get("items.find { it.productId == 'PROD-002' }.lineTotal"), "499400.10", "lineTotal PROD-002");
        assertAmount(order.get("items.find { it.productId == 'PROD-003' }.lineTotal"), "649250.10", "lineTotal PROD-003");
        assertTotals(order, "4645150.20", "464515.02", "4180635.18");
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-014 — produto com histórico usa o preço da janela vigente (649,90, não 700,00)")
    void currentPriceWindowIsUsed() {
        JsonPath order = Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(KEYBOARD, 1)).jsonPath();

        assertAmount(order.get("items[0].unitPrice"), KEYBOARD_PRICE, "unitPrice da janela vigente");
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-015 — preço, total, status e id enviados pelo cliente são ignorados")
    void clientSuppliedPricingFieldsAreIgnored() {
        String forgedId = UUID.randomUUID().toString();
        Map<String, Object> forgedItem = new LinkedHashMap<>();
        forgedItem.put("productId", NOTEBOOK);
        forgedItem.put("quantity", 1);
        forgedItem.put("unitPrice", 0.01);
        forgedItem.put("productName", "Grátis");
        forgedItem.put("lineTotal", 0.01);

        Instant before = Instant.now();
        JsonPath order = Orders.createAccepted(OrderPayload.of(TestClient.unique())
                .item(forgedItem)
                .rootField("subtotal", 0.01)
                .rootField("discount", 3500)
                .rootField("total", 0)
                .rootField("status", "PAGO")
                .rootField("id", forgedId)
                .rootField("createdAt", "2000-01-01T00:00:00Z")).jsonPath();

        assertAmount(order.get("items[0].unitPrice"), NOTEBOOK_PRICE, "unitPrice vem do catálogo");
        assertThat(order.getString("items[0].productName")).isEqualTo(NOTEBOOK_NAME);
        assertTotals(order, "3500.00", "350.00", "3150.00");
        assertThat(order.getString("status")).isEqualTo("CRIADO");
        assertThat(order.getString("id")).isNotEqualTo(forgedId);
        assertThat(Instant.parse(order.getString("createdAt"))).isAfter(before.minus(Duration.ofMinutes(5)));
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-016 — contrato da resposta de criação (Location, UUID, createdAt UTC, schema)")
    void creationResponseContract() {
        Instant testClock = Instant.now();
        Response response = Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(NOTEBOOK, 1));

        String id = response.jsonPath().getString("id");
        assertThat(response.header("Location")).as("header Location").endsWith("/api/v1/orders/" + id);
        assertThat(response.contentType()).startsWith("application/json");
        ApiAssertions.assertMatchesSchema(response, "order.json");
        Instant createdAt = Instant.parse(response.jsonPath().getString("createdAt"));
        assertThat(Duration.between(testClock, createdAt).abs()).as("createdAt próximo do relógio do teste")
                .isLessThan(Duration.ofMinutes(5));
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-017 — pedido criado fica persistido e idêntico na consulta")
    @KnownDefect("H-10")
    void createdOrderIsPersistedIdentically() {
        Response created = Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(NOTEBOOK, 2).item(MOUSE, 1));

        Response fetched = Orders.get(created.jsonPath().getString("id"));

        assertThat(fetched.statusCode()).isEqualTo(200);
        assertThat(fetched.jsonPath().getMap("$")).as("GET /orders/{id} idêntico à resposta de criação (RN-16)")
                .isEqualTo(created.jsonPath().getMap("$"));
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-018 — caracteres especiais e Unicode no cliente são preservados")
    void unicodeClientDataIsPreserved() {
        TestClient base = TestClient.unique();
        String name = "José Ação Ñúñez 李雷 " + base.email().substring(3, 12);
        Response created = Orders.createAccepted(OrderPayload.of(base)
                .clientField("name", name).clientField("document", "123.456.789-01").item(MOUSE, 1));

        JsonPath fetched = Orders.get(created.jsonPath().getString("id")).jsonPath();

        assertThat(created.jsonPath().getString("client.name")).isEqualTo(name);
        assertThat(fetched.getString("client.name")).isEqualTo(name);
        assertThat(fetched.getString("client.document")).isEqualTo("123.456.789-01");
    }

    private static void assertTotals(JsonPath order, String subtotal, String discount, String total) {
        assertAmount(order.get("subtotal"), subtotal, "subtotal");
        assertAmount(order.get("discount"), discount, "discount");
        assertAmount(order.get("total"), total, "total");
    }
}
