package com.openlab.qualitos.quality.riskregister.domain;

public enum RegisterEventType {
    CREATED,
    /** Cotation brute (risque) ou évaluation gain × faisabilité (opportunité). */
    RATING_CHANGED,
    /** Cotation résiduelle visée d'un risque. */
    RESIDUAL_CHANGED,
    STATUS_CHANGED,
    DECISION_CHANGED,
    ACTION_OPENED
}
