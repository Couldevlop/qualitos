package com.openlab.qualitos.quality.dpia.domain;

/**
 * Niveau de risque résiduel d'une DPIA (post-mitigation).
 * <p>
 * HIGH / SEVERE déclenchent l'obligation de consultation préalable à
 * l'autorité de contrôle (RGPD Art. 36§1) — invariant validé par l'agrégat.
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    SEVERE;

    public boolean requiresPriorConsultation() {
        return this == HIGH || this == SEVERE;
    }
}
