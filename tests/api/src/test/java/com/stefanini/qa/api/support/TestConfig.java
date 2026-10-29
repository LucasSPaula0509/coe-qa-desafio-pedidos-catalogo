package com.stefanini.qa.api.support;

/**
 * Endereços do ambiente sob teste. Cada valor pode ser sobrescrito por propriedade de sistema
 * (-DORDER_BASE_URL=...) ou variável de ambiente; os padrões correspondem ao Docker Compose local
 * com o override de teste (tests/infra/docker-compose.qa.yml).
 */
public final class TestConfig {

    public static final String ORDER_BASE_URL = value("ORDER_BASE_URL", "http://localhost:8080");
    public static final String CATALOG_BASE_URL = value("CATALOG_BASE_URL", "http://localhost:8081");
    public static final String WEB_BASE_URL = value("WEB_BASE_URL", "http://localhost:4200");
    /** Container do order-service: os testes de segurança inspecionam o log JSON (CT-SEC-009). */
    public static final String ORDER_SERVICE_CONTAINER = value("ORDER_SERVICE_CONTAINER", "sut-order-service-1");
    public static final String CATALOG_PROXY_ADMIN_URL = value("CATALOG_PROXY_ADMIN_URL", "http://localhost:8089");
    /** Container do proxy, parado em CT-INT-015 para simular destino inacessível. */
    public static final String CATALOG_PROXY_CONTAINER = value("CATALOG_PROXY_CONTAINER", "sut-catalog-proxy-1");

    public static final String POSTGRES_URL = value("POSTGRES_URL", "jdbc:postgresql://localhost:5432/orders");
    public static final String POSTGRES_USER = value("POSTGRES_USER", "orders");
    public static final String POSTGRES_PASSWORD = value("POSTGRES_PASSWORD", "orders");

    public static final String MONGODB_URI = value("MONGODB_URI", "mongodb://localhost:27017");
    public static final String MONGODB_DATABASE = value("MONGODB_DATABASE", "catalog");

    private TestConfig() {
    }

    private static String value(String name, String defaultValue) {
        String fromProperty = System.getProperty(name);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        String fromEnv = System.getenv(name);
        return fromEnv != null && !fromEnv.isBlank() ? fromEnv : defaultValue;
    }
}
