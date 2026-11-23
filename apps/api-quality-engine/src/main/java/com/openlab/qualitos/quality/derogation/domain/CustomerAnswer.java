package com.openlab.qualitos.quality.derogation.domain;

import java.time.LocalDate;

/**
 * Section 6 — la réponse du client, portée à la main.
 *
 * <p>Ni nom ni signature du client ne sont demandés : la pièce opposable est le
 * PDF qu'il a signé de son côté et renvoyé par courriel. Cette section est de la
 * <b>traçabilité interne</b>.
 */
public record CustomerAnswer(
        CustomerDecision decision,
        LocalDate decidedOn
) {
    public CustomerAnswer {
        decision = decision == null ? CustomerDecision.PENDING : decision;
    }

    /** Aucune réponse encore. */
    public static CustomerAnswer enAttente() {
        return new CustomerAnswer(CustomerDecision.PENDING, null);
    }
}
