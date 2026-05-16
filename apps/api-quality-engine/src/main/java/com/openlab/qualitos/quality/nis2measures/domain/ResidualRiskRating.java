package com.openlab.qualitos.quality.nis2measures.domain;

/**
 * Niveau de risque résiduel après mise en œuvre de la mesure. CRITICAL exige
 * une attention prioritaire (escalade direction).
 */
public enum ResidualRiskRating {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public boolean requiresExecutiveAttention() {
        return this == CRITICAL;
    }
}
