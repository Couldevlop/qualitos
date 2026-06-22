package com.openlab.qualitos.quality.academy.domain;

/**
 * État d'une inscription à un cours e-learning.
 *
 * <p>Transitions : {@code ENROLLED → IN_PROGRESS → (COMPLETED | FAILED)} ;
 * {@code ENROLLED | IN_PROGRESS → CANCELLED}. Les états terminaux
 * ({@code COMPLETED}, {@code FAILED}, {@code CANCELLED}) sont définitifs.</p>
 */
public enum AcademyEnrollmentStatus {
    ENROLLED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
