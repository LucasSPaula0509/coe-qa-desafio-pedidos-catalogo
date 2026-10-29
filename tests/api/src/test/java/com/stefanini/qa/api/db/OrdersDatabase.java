package com.stefanini.qa.api.db;

import com.stefanini.qa.api.support.TestConfig;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso direto ao PostgreSQL do order-service para verificar persistência (all-or-nothing) e
 * preparar a regra de desconto (schema em sut/docs/database.md). Consultas sempre parametrizadas.
 */
public final class OrdersDatabase {

    private OrdersDatabase() {
    }

    /** Quantidade de pedidos gravados para o e-mail do cliente. */
    public static long countOrders(String clientEmail) {
        return queryLong("SELECT COUNT(*) FROM orders WHERE client_email = ?", clientEmail);
    }

    /** Quantidade de itens gravados em pedidos do e-mail do cliente (detecta persistência parcial). */
    public static long countItems(String clientEmail) {
        return queryLong("""
                SELECT COUNT(*) FROM order_items i JOIN orders o ON o.id = i.order_id
                WHERE o.client_email = ?""", clientEmail);
    }

    public static long countAllOrders() {
        return queryLong("SELECT COUNT(*) FROM orders");
    }

    public static List<DiscountRuleRow> discountRules() {
        List<DiscountRuleRow> rules = new ArrayList<>();
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, minimum_amount, percentage, active FROM discount_rule ORDER BY id");
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rules.add(new DiscountRuleRow(rs.getLong("id"), rs.getBigDecimal("minimum_amount"),
                        rs.getBigDecimal("percentage"), rs.getBoolean("active")));
            }
            return rules;
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao ler discount_rule", e);
        }
    }

    public static void deactivateAllDiscountRules() {
        execute("UPDATE discount_rule SET active = FALSE");
    }

    /** Insere uma regra com created_at = now() (mais recente que as existentes). */
    public static void insertDiscountRule(String minimumAmount, String percentage, boolean active) {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO discount_rule (minimum_amount, percentage, active) VALUES (?, ?, ?)")) {
            statement.setBigDecimal(1, new BigDecimal(minimumAmount));
            statement.setBigDecimal(2, new BigDecimal(percentage));
            statement.setBoolean(3, active);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao inserir discount_rule", e);
        }
    }

    /** Deixa ativa somente uma nova regra com os valores informados. */
    public static void useOnlyDiscountRule(String minimumAmount, String percentage) {
        deactivateAllDiscountRules();
        insertDiscountRule(minimumAmount, percentage, true);
    }

    /** Restaura a tabela ao snapshot: remove regras criadas depois e repõe os valores originais. */
    public static void restoreDiscountRules(List<DiscountRuleRow> snapshot) {
        long maxId = snapshot.stream().mapToLong(DiscountRuleRow::id).max().orElse(0);
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM discount_rule WHERE id > ?");
                    PreparedStatement update = connection.prepareStatement(
                            "UPDATE discount_rule SET minimum_amount = ?, percentage = ?, active = ? WHERE id = ?")) {
                delete.setLong(1, maxId);
                delete.executeUpdate();
                for (DiscountRuleRow rule : snapshot) {
                    update.setBigDecimal(1, rule.minimumAmount());
                    update.setBigDecimal(2, rule.percentage());
                    update.setBoolean(3, rule.active());
                    update.setLong(4, rule.id());
                    update.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao restaurar discount_rule", e);
        }
    }

    private static long queryLong(String sql, Object... parameters) {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha na consulta: " + sql, e);
        }
    }

    private static void execute(String sql) {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao executar: " + sql, e);
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(TestConfig.POSTGRES_URL, TestConfig.POSTGRES_USER,
                TestConfig.POSTGRES_PASSWORD);
    }

    public record DiscountRuleRow(long id, BigDecimal minimumAmount, BigDecimal percentage, boolean active) {
    }
}
