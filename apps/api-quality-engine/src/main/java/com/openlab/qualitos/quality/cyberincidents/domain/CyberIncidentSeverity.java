package com.openlab.qualitos.quality.cyberincidents.domain;

/**
 * Sévérité d'un incident cyber. HIGH/CRITICAL : seuils de notification CSIRT
 * activés (Art. 23 NIS2 — "incident significatif").
 */
public enum CyberIncidentSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public boolean isSignificant() {
        return this == HIGH || this == CRITICAL;
    }
}
