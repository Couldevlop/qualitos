package com.openlab.qualitos.quality.riskregister.domain;

/**
 * Les exigences normatives qu'une fiche peut couvrir.
 *
 * <p>Catalogue fermé : la fiche comptera comme preuve de ces clauses dans la
 * matrice du SMI, et une clause tapée à la main (« ISO9001 6.1 », « 9001-6.1 »)
 * ne s'y rattacherait jamais.
 *
 * <p>IATF 16949 §6.1.2 porte sur l'analyse des risques : il n'est proposé
 * qu'aux risques. ISO 9001 §10.3 (amélioration continue) ne vaut que pour les
 * opportunités.
 */
public enum RegisterRequirement {
    ISO_9001_6_1(true, true),
    ISO_9001_10_3(false, true),
    ISO_14001_6_1(true, true),
    ISO_45001_6_1(true, true),
    IATF_16949_6_1_2(true, false);

    private final boolean forRisk;
    private final boolean forOpportunity;

    RegisterRequirement(boolean forRisk, boolean forOpportunity) {
        this.forRisk = forRisk;
        this.forOpportunity = forOpportunity;
    }

    public boolean forRisk() {
        return forRisk;
    }

    public boolean forOpportunity() {
        return forOpportunity;
    }
}
