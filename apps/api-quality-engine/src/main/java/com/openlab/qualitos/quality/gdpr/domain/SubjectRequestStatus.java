package com.openlab.qualitos.quality.gdpr.domain;

/**
 * RECEIVED → IN_PROGRESS → (COMPLETED | REJECTED).
 * COMPLETED et REJECTED sont terminaux.
 */
public enum SubjectRequestStatus {
    RECEIVED, IN_PROGRESS, COMPLETED, REJECTED
}
