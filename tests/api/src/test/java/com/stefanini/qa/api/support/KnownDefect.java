package com.stefanini.qa.api.support;

import io.qameta.allure.LinkAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;

/**
 * Marca um teste que reproduz um defeito confirmado e registrado como Issue. Política da
 * estratégia (§12): o teste continua executando e falhando visivelmente; nunca é desativado.
 * A tag "defeito-conhecido" permite ao gate de CI avaliar a falha pela severidade do defeito, e o
 * relatório Allure ganha o link para a Issue (allure.link.issue.pattern).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Tag("defeito-conhecido")
@LinkAnnotation(type = "issue")
public @interface KnownDefect {

    /** Número da Issue do defeito no GitHub (ex.: "5"). */
    String value();

    /** Suspeita confirmada na especificação (specs/open-questions.md), ex.: "H-11". */
    String suspicion();
}
