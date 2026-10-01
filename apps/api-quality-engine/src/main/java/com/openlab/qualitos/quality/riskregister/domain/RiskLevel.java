package com.openlab.qualitos.quality.riskregister.domain;

/** Le niveau d'un risque : Faible 1–4, Moyen 5–9, Élevé 10–14, Critique 15–25. */
public enum RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL;

    public static RiskLevel of(int score) {
        if (score >= 15) return CRITICAL;
        if (score >= 10) return HIGH;
        if (score >= 5) return MEDIUM;
        return LOW;
    }
}
