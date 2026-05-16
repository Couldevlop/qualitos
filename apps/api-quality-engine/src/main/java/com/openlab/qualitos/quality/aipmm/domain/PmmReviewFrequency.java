package com.openlab.qualitos.quality.aipmm.domain;

import java.time.Duration;

/**
 * Fréquence de revue d'un plan PMM. Détermine la prochaine échéance
 * {@code nextReviewDueAt = lastReviewedAt + period}.
 */
public enum PmmReviewFrequency {
    WEEKLY(Duration.ofDays(7)),
    MONTHLY(Duration.ofDays(30)),
    QUARTERLY(Duration.ofDays(90)),
    SEMI_ANNUAL(Duration.ofDays(182)),
    ANNUAL(Duration.ofDays(365));

    private final Duration period;
    PmmReviewFrequency(Duration period) { this.period = period; }
    public Duration period() { return period; }
}
