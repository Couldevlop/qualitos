package com.openlab.qualitos.quality.breach.domain;

/**
 * Gravité estimée d'une violation. HIGH/CRITICAL déclenchent l'obligation
 * de notifier les personnes concernées (Art. 34).
 */
public enum BreachSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public boolean requiresSubjectNotification() {
        return this == HIGH || this == CRITICAL;
    }
}
