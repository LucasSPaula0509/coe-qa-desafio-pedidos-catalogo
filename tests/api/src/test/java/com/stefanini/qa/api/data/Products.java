package com.stefanini.qa.api.data;

/**
 * Massa inicial do catálogo (RN-22, specs/business-rules.md). É o oráculo dos casos de teste:
 * os valores vêm da especificação, não de consultas ao sistema.
 */
public final class Products {

    /** Ativo, 3500.00 vigente desde 2025-01-01. */
    public static final String NOTEBOOK = "PROD-001";
    public static final String NOTEBOOK_NAME = "Notebook Dell Inspiron 15";
    public static final String NOTEBOOK_PRICE = "3500.00";

    /** Ativo, 499.90 vigente desde 2025-01-01. */
    public static final String MOUSE = "PROD-002";
    public static final String MOUSE_NAME = "Mouse Logitech MX Master 3S";
    public static final String MOUSE_PRICE = "499.90";

    /** Ativo, histórico: 700.00 expirado em 2025-06-01; 649.90 vigente desde então. */
    public static final String KEYBOARD = "PROD-003";
    public static final String KEYBOARD_NAME = "Teclado Mecanico Keychron K2";
    public static final String KEYBOARD_PRICE = "649.90";
    public static final String KEYBOARD_PRICE_VALID_FROM = "2025-06-01T00:00:00Z";

    /** INATIVO, 1200.00. */
    public static final String MONITOR_INACTIVE = "PROD-004";
    public static final String MONITOR_PRICE = "1200.00";

    /** Ativo, preço apenas futuro (2030-01-01): sem preço vigente. */
    public static final String WEBCAM_FUTURE_PRICE = "PROD-005";

    /** Ativo, preço expirado (2025-03-01): sem preço vigente. */
    public static final String HEADSET_EXPIRED_PRICE = "PROD-006";

    /** Inexistente no catálogo. */
    public static final String MISSING = "PROD-999";

    public static final String CURRENCY = "BRL";
    public static final String SEED_VALID_FROM = "2025-01-01T00:00:00Z";

    private Products() {
    }
}
