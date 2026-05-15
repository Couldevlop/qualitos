package com.openlab.qualitos.quality.webhooks;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RetryBackoffTest {

    private final RetryBackoff backoff = new RetryBackoff();

    @Test
    void firstAttempt_30s() {
        long delaySec = backoff.nextRetryAt(1).getEpochSecond() - Instant.now().getEpochSecond();
        assertThat(delaySec).isBetween(25L, 35L);
    }

    @Test
    void secondAttempt_2min() {
        long delaySec = backoff.nextRetryAt(2).getEpochSecond() - Instant.now().getEpochSecond();
        assertThat(delaySec).isBetween(115L, 125L);
    }

    @Test
    void clampsToMaxOnHighAttemptCount() {
        // 100ème essai → plafonné au dernier delay (24h)
        Instant retry = backoff.nextRetryAt(100);
        long delaySec = retry.getEpochSecond() - Instant.now().getEpochSecond();
        assertThat(delaySec).isLessThanOrEqualTo(86405);
        assertThat(delaySec).isGreaterThan(86395);
    }

    @Test
    void zeroOrNegative_treatedAsFirstAttempt() {
        long delaySec0 = backoff.nextRetryAt(0).getEpochSecond() - Instant.now().getEpochSecond();
        long delaySec1 = backoff.nextRetryAt(1).getEpochSecond() - Instant.now().getEpochSecond();
        assertThat(Math.abs(delaySec0 - delaySec1)).isLessThan(2);
    }
}
