package com.stefanini.qa.api.orders;

import static com.stefanini.qa.api.data.Products.MOUSE;
import static com.stefanini.qa.api.data.Products.NOTEBOOK;
import static com.stefanini.qa.api.support.Money.assertAmount;
import static org.assertj.core.api.Assertions.assertThat;

import com.stefanini.qa.api.data.OrderPayload;
import com.stefanini.qa.api.data.TestClient;
import com.stefanini.qa.api.db.OrdersDatabase;
import com.stefanini.qa.api.db.OrdersDatabase.DiscountRuleRow;
import com.stefanini.qa.api.support.Orders;
import io.restassured.path.json.JsonPath;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * docs/test-cases/api-orders.md — A. Criação e cálculo, casos que exigem ajustar a regra de desconto
 * (Q-09): a massa não atinge R$ 1.000,00 exatos e 10% sobre preços terminados em 0 centavos nunca
 * exercita o arredondamento. A tabela discount_rule é restaurada após cada caso, mesmo em falha.
 */
@Tag("api")
@Tag("db")
@Tag("serial")
@DisplayName("Pedidos — regra de desconto configurável")
class DiscountRuleTest {

    private List<DiscountRuleRow> snapshot;

    @BeforeEach
    void snapshotDiscountRules() {
        snapshot = OrdersDatabase.discountRules();
    }

    @AfterEach
    void restoreDiscountRules() {
        OrdersDatabase.restoreDiscountRules(snapshot);
        assertThat(OrdersDatabase.discountRules()).as("regra de desconto restaurada").isEqualTo(snapshot);
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-005 — subtotal exatamente igual ao mínimo recebe desconto (\"a partir de\")")
    void subtotalEqualToMinimumGetsDiscount() {
        OrdersDatabase.useOnlyDiscountRule("999.80", "10.00");

        JsonPath order = createOrder(MOUSE, 2);

        assertTotals(order, "999.80", "99.98", "899.82");
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-006 — subtotal 1 centavo abaixo do mínimo não recebe desconto")
    void subtotalOneCentBelowMinimumGetsNoDiscount() {
        OrdersDatabase.useOnlyDiscountRule("999.81", "10.00");

        JsonPath order = createOrder(MOUSE, 2);

        assertTotals(order, "999.80", "0.00", "999.80");
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-007 — desconto arredondado HALF_UP (74,985 → 74,99; HALF_EVEN daria 74,98)")
    void discountIsRoundedHalfUp() {
        OrdersDatabase.useOnlyDiscountRule("1000.00", "5.00");

        JsonPath order = createOrder(MOUSE, 3);

        assertTotals(order, "1499.70", "74.99", "1424.71");
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-008 — desconto arredondado para baixo quando a terceira casa é menor que 5 (44,991 → 44,99)")
    void discountIsRoundedDownBelowHalf() {
        OrdersDatabase.useOnlyDiscountRule("1000.00", "3.00");

        JsonPath order = createOrder(MOUSE, 3);

        assertTotals(order, "1499.70", "44.99", "1454.71");
    }

    @Test
    @Tag("P1")
    @DisplayName("CT-ORD-009 — sem regra de desconto ativa o desconto é zero")
    void noActiveRuleMeansNoDiscount() {
        OrdersDatabase.deactivateAllDiscountRules();

        JsonPath order = createOrder(NOTEBOOK, 1);

        assertTotals(order, "3500.00", "0.00", "3500.00");
    }

    @Test
    @Tag("P2")
    @DisplayName("CT-ORD-010 — com mais de uma regra ativa vale a mais antiga")
    void oldestActiveRuleWins() {
        OrdersDatabase.insertDiscountRule("0.00", "50.00", true);

        JsonPath order = createOrder(NOTEBOOK, 1);

        assertTotals(order, "3500.00", "350.00", "3150.00");
    }

    @Test
    @Tag("P3")
    @DisplayName("CT-ORD-011 — percentual máximo (100%) resulta em total zero")
    void fullDiscountResultsInZeroTotal() {
        OrdersDatabase.useOnlyDiscountRule("0.00", "100.00");

        JsonPath order = createOrder(MOUSE, 1);

        assertTotals(order, "499.90", "499.90", "0.00");
    }

    private static JsonPath createOrder(String productId, int quantity) {
        return Orders.createAccepted(OrderPayload.of(TestClient.unique()).item(productId, quantity)).jsonPath();
    }

    private static void assertTotals(JsonPath order, String subtotal, String discount, String total) {
        assertAmount(order.get("subtotal"), subtotal, "subtotal");
        assertAmount(order.get("discount"), discount, "discount");
        assertAmount(order.get("total"), total, "total");
    }
}
