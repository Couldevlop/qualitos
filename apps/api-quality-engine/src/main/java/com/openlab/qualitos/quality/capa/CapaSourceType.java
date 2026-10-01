package com.openlab.qualitos.quality.capa;

public enum CapaSourceType {
    NON_CONFORMITY, AUDIT, COMPLAINT, INTERNAL, IOT_ALERT, SPC_ALERT,
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
