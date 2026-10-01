package com.openlab.qualitos.quality.riskregister.domain;

/**
 * D'où vient la ligne.
 *
 * <p>Une opportunité ne naît pas d'une AMDEC, d'une non-conformité, d'un
 * incident ni de l'analyse d'impact d'un changement : ces sources analysent ce
 * qui peut mal tourner. Elles ne sont donc proposées qu'aux risques.
 */
public enum RegisterOrigin {
    DIRECT(true),
    MANAGEMENT_REVIEW(true),
    AUDIT(true),
    CUSTOMER_FEEDBACK(true),
    MONITORING(true),
    FMEA(false),
    NON_CONFORMITY(false),
    INCIDENT(false),
    /** Analyse d'impact d'une demande de changement (MOC). */
    CHANGE(false);

    private final boolean forOpportunity;

    RegisterOrigin(boolean forOpportunity) {
        this.forOpportunity = forOpportunity;
    }

    public boolean forOpportunity() {
        return forOpportunity;
    }

    /** Vrai pour une origine qui désigne un objet réel de la plateforme, vérifiable. */
    public boolean hasSource() {
        return this == FMEA || this == NON_CONFORMITY || this == AUDIT || this == CHANGE;
    }
}
