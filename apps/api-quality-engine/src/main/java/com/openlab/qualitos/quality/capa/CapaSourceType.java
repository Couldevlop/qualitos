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
    OTHER
}
