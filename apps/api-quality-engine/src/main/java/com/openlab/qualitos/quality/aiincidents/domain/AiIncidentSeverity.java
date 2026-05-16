package com.openlab.qualitos.quality.aiincidents.domain;

import java.time.Duration;

/**
 * Sévérité d'incident IA selon AI Act Art. 73.
 *
 * Délais de notification au régulateur :
 *  - DEATH_OR_SERIOUS_HARM_TO_HEALTH : sans délai et au plus 2 jours.
 *  - SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS : sans délai et au plus 10 jours.
 *  - CRITICAL_INFRASTRUCTURE_DISRUPTION : sans délai et au plus 15 jours.
 *  - SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE : au plus 15 jours.
 */
public enum AiIncidentSeverity {
    DEATH_OR_SERIOUS_HARM_TO_HEALTH(Duration.ofDays(2)),
    SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS(Duration.ofDays(10)),
    CRITICAL_INFRASTRUCTURE_DISRUPTION(Duration.ofDays(15)),
    SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE(Duration.ofDays(15));

    private final Duration regulatorNotificationDeadline;

    AiIncidentSeverity(Duration regulatorNotificationDeadline) {
        this.regulatorNotificationDeadline = regulatorNotificationDeadline;
    }

    public Duration regulatorNotificationDeadline() {
        return regulatorNotificationDeadline;
    }
}
