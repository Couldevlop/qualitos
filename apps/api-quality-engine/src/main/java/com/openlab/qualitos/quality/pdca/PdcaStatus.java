package com.openlab.qualitos.quality.pdca;

public enum PdcaStatus {
    PLAN,
    DO,
    CHECK,
    ACT,
    COMPLETED,
    CANCELLED;

    /**
     * Retourne le statut suivant dans la séquence PLAN → DO → CHECK → ACT → COMPLETED.
     * Lève une exception si le cycle est déjà terminé ou annulé.
     */
    public PdcaStatus next() {
        return switch (this) {
            case PLAN -> DO;
            case DO -> CHECK;
            case CHECK -> ACT;
            case ACT -> COMPLETED;
            case COMPLETED -> throw new PdcaStateException("Cycle is already completed");
            case CANCELLED -> throw new PdcaStateException("Cancelled cycle cannot advance");
        };
    }

    /**
     * Retourne la phase correspondant au statut courant, ou null si le cycle n'est pas dans une phase active.
     */
    public PdcaPhase toPhase() {
        return switch (this) {
            case PLAN -> PdcaPhase.PLAN;
            case DO -> PdcaPhase.DO;
            case CHECK -> PdcaPhase.CHECK;
            case ACT -> PdcaPhase.ACT;
            default -> null;
        };
    }
}
