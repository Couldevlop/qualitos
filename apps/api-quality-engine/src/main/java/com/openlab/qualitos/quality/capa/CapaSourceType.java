package com.openlab.qualitos.quality.capa;

public enum CapaSourceType {
    /** Écart constaté en INTERNE : autocontrôle, audit interne, revue. */
    NON_CONFORMITY,
    /**
     * Écart signalé du DEHORS et instruit dans le registre des écarts : une
     * réclamation client ouverte en « NC externe ».
     *
     * <p>Distinguée de {@link #NON_CONFORMITY} parce que c'est la première
     * question d'une revue de direction, et parce que l'écran affichait
     * « Non-conformité interne » sur des dossiers nés d'une réclamation client —
     * l'inverse de la vérité, sur la fiche même qui sert à répondre au client.
     *
     * <p>Distinguée aussi de {@link #COMPLAINT}, qui désigne le registre des
     * réclamations (§4.9) : ici {@code sourceRef} porte la référence d'un écart
     * ({@code Ext-NC-2026-0004}), là celle d'une réclamation.
     */
    EXTERNAL_NON_CONFORMITY,
    AUDIT, COMPLAINT, INTERNAL, IOT_ALERT, SPC_ALERT,
    /**
     * Anomalie détectée par apprentissage non supervisé (ADR 0022 : Isolation
     * Forest, reconstruction par ACP). Distinguée de {@code SPC_ALERT}, qui
     * relève d'une règle statistique nommée et explicable en soi : ici la
     * détection est multivariée, et l'auditeur doit pouvoir savoir d'où vient
     * l'alerte pour juger de sa valeur.
     */
    ANOMALY,
    /**
     * Dossier ouvert depuis une fiche du registre des risques (ISO 9001 §6.1) :
     * {@code sourceRef} porte la référence du risque (R-014). C'est ce couple
     * qui permet à la fiche de retrouver les actions qui la traitent.
     */
    RISK,
    OTHER
}
