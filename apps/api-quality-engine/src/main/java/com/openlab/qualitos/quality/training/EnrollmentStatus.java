package com.openlab.qualitos.quality.training;

/**
 * Cycle de vie d'une inscription à un parcours.
 *
 * ENROLLED → IN_PROGRESS → (COMPLETED | FAILED)
 * ENROLLED | IN_PROGRESS → CANCELLED
 * Tout est terminal sauf ENROLLED et IN_PROGRESS.
 */
public enum EnrollmentStatus {
    ENROLLED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}
