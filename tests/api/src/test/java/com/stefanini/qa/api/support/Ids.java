package com.stefanini.qa.api.support;

import java.util.concurrent.ThreadLocalRandom;

/** Identificadores únicos por teste, para isolar dados num ambiente com pedidos acumulados. */
public final class Ids {

    private Ids() {
    }

    /** Identificador curto, único e seguro para e-mail/nome (ex.: "r1m3k9x0a7b2"). */
    public static String runId() {
        return "r" + Long.toString(System.currentTimeMillis(), 36)
                + Integer.toString(ThreadLocalRandom.current().nextInt(36 * 36 * 36 * 36), 36);
    }

    /** IDs de produto inexistentes e distintos: QA-NX-001, QA-NX-002, ... */
    public static String missingProduct(int sequence) {
        return "QA-NX-%03d".formatted(sequence);
    }
}
