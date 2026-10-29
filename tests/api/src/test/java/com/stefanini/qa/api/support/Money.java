package com.stefanini.qa.api.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

/** Comparação de valores monetários por valor numérico (escala ignorada: 1149.8 == 1149.80). */
public final class Money {

    private Money() {
    }

    public static BigDecimal of(String value) {
        return new BigDecimal(value);
    }

    public static void assertAmount(Object actual, String expected, String description) {
        assertThat(actual).as(description).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) actual).as(description).isEqualByComparingTo(expected);
    }
}
