package com.openlab.qualitos.quality.standards;

/**
 * Statut d'une étape de la roadmap de certification.
 * NOT_STARTED → IN_PROGRESS → DONE ; SKIPPED pour une étape non applicable.
 */
public enum StageStatus {
    NOT_STARTED,
    IN_PROGRESS,
    DONE,
    SKIPPED
}
