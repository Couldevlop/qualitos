package com.openlab.qualitos.quality.costofquality.domain;

/**
 * Les quatre familles du modèle PAF (Prévention – Appréciation – Défaillances).
 *
 * <p>Les deux premières sont des coûts de CONFORMITÉ : une dépense choisie pour
 * que le défaut n'arrive pas, ou qu'il soit vu avant de sortir. Les deux
 * dernières sont des coûts de NON-CONFORMITÉ : une perte subie parce qu'il est
 * arrivé. Le ratio que l'écran affiche oppose ces deux moitiés.
 */
public enum CoqCategory {
    PREVENTION(true),
    APPRAISAL(true),
    INTERNAL_FAILURE(false),
    EXTERNAL_FAILURE(false);

    private final boolean conformance;

    CoqCategory(boolean conformance) {
        this.conformance = conformance;
    }

    /** @return vrai pour une dépense de conformité (investissement), faux pour une perte. */
    public boolean conformance() {
        return conformance;
    }
}
