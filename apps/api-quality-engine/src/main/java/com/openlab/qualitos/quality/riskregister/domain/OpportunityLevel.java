package com.openlab.qualitos.quality.riskregister.domain;

/**
 * Le niveau d'une opportunité : Faible 1–4, Moyen 5–9, Élevé 10–14, Prioritaire 15–25.
 *
 * <p>Mêmes seuils que le risque, autre mot au sommet : une opportunité forte
 * n'est pas « critique », elle est à saisir en premier.
 */
public enum OpportunityLevel {
    LOW, MEDIUM, HIGH, PRIORITY;

    public static OpportunityLevel of(int score) {
        if (score >= 15) return PRIORITY;
        if (score >= 10) return HIGH;
        if (score >= 5) return MEDIUM;
        return LOW;
    }
}
