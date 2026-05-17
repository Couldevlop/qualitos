package com.openlab.qualitos.quality.aiconformity.domain;

/**
 * Cycle de vie d'une évaluation de conformité.
 *
 *   PLANNED → IN_PROGRESS → CERTIFIED → EXPIRED
 *   PLANNED|IN_PROGRESS|CERTIFIED → REVOKED
 *   PLANNED|IN_PROGRESS → FAILED
 */
public enum ConformityAssessmentStatus {
    PLANNED,
    IN_PROGRESS,
    CERTIFIED,
    EXPIRED,
    REVOKED,
    FAILED
}
