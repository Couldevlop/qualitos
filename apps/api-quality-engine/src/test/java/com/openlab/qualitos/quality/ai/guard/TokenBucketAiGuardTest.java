package com.openlab.qualitos.quality.ai.guard;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests des garde-fous IA (OWASP LLM04) avec une horloge contrôlée :
 * débit (token bucket), quota journalier, taille de prompt et disjoncteur.
 */
class TokenBucketAiGuardTest {

    private static final String TENANT = "11111111-1111-1111-1111-111111111111";

    private AiCallContext ctx(int promptChars) {
        return new AiCallContext(TENANT, "complete", promptChars);
    }

    private AiGuardProperties props(int rpm, int dailyQuota, int maxPrompt, int failures, long openSec) {
        AiGuardProperties p = new AiGuardProperties();
        p.setEnabled(true);
        p.setRequestsPerMinute(rpm);
        p.setDailyQuota(dailyQuota);
        p.setMaxPromptChars(maxPrompt);
        p.setCircuitFailureThreshold(failures);
        p.setCircuitOpenSeconds(openSec);
        return p;
    }

    @Test
    void allowsUpToRatePerMinuteThenRejects() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T10:00:00Z"));
        TokenBucketAiGuard guard = new TokenBucketAiGuard(props(3, 100, 1000, 5, 30), clock);

        for (int i = 0; i < 3; i++) {
            assertThatCode(() -> guard.check(ctx(10))).doesNotThrowAnyException();
        }
        assertThatThrownBy(() -> guard.check(ctx(10)))
                .isInstanceOf(AiRateLimitExceededException.class);
    }

    @Test
    void refillsBucketAsTimePasses() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T10:00:00Z"));
        TokenBucketAiGuard guard = new TokenBucketAiGuard(props(3, 100, 1000, 5, 30), clock);

        for (int i = 0; i < 3; i++) {
            guard.check(ctx(10));
        }
        // Bucket vide : un refill (20 s -> 1 jeton a 3/min) doit re-autoriser un appel.
        clock.advance(Duration.ofSeconds(20));
        assertThatCode(() -> guard.check(ctx(10))).doesNotThrowAnyException();
    }

    @Test
    void enforcesDailyQuota() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T10:00:00Z"));
        // rpm eleve pour isoler le quota (2/jour).
        TokenBucketAiGuard guard = new TokenBucketAiGuard(props(100, 2, 1000, 5, 30), clock);

        guard.check(ctx(10));
        guard.check(ctx(10));
        assertThatThrownBy(() -> guard.check(ctx(10)))
                .isInstanceOf(AiQuotaExceededException.class);
    }

    @Test
    void quotaResetsOnNextDay() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T10:00:00Z"));
        TokenBucketAiGuard guard = new TokenBucketAiGuard(props(100, 1, 1000, 5, 30), clock);

        guard.check(ctx(10));
        assertThatThrownBy(() -> guard.check(ctx(10)))
                .isInstanceOf(AiQuotaExceededException.class);

        clock.advance(Duration.ofDays(1));
        assertThatCode(() -> guard.check(ctx(10))).doesNotThrowAnyException();
    }

    @Test
    void rejectsOversizedPrompt() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T10:00:00Z"));
        TokenBucketAiGuard guard = new TokenBucketAiGuard(props(100, 100, 50, 5, 30), clock);

        assertThatThrownBy(() -> guard.check(ctx(51)))
                .isInstanceOf(AiPromptTooLargeException.class);
        assertThatCode(() -> guard.check(ctx(50))).doesNotThrowAnyException();
    }

    @Test
    void opensCircuitAfterConsecutiveFailures() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T10:00:00Z"));
        TokenBucketAiGuard guard = new TokenBucketAiGuard(props(100, 100, 1000, 2, 30), clock);

        guard.recordFailure(TENANT);
        guard.recordFailure(TENANT);
        assertThatThrownBy(() -> guard.check(ctx(10)))
                .isInstanceOf(AiCircuitOpenException.class);
    }

    @Test
    void halfOpensAfterCooldownThenClosesOnSuccess() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T10:00:00Z"));
        TokenBucketAiGuard guard = new TokenBucketAiGuard(props(100, 100, 1000, 2, 30), clock);

        guard.recordFailure(TENANT);
        guard.recordFailure(TENANT);
        clock.advance(Duration.ofSeconds(31)); // refroidissement ecoule -> demi-ouvert

        assertThatCode(() -> guard.check(ctx(10))).doesNotThrowAnyException(); // appel d'essai admis
        guard.recordSuccess(TENANT);                                          // referme
        assertThatCode(() -> guard.check(ctx(10))).doesNotThrowAnyException();
    }

    @Test
    void halfOpenFailureReopensCircuit() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T10:00:00Z"));
        TokenBucketAiGuard guard = new TokenBucketAiGuard(props(100, 100, 1000, 2, 30), clock);

        guard.recordFailure(TENANT);
        guard.recordFailure(TENANT);
        clock.advance(Duration.ofSeconds(31));
        guard.check(ctx(10));        // demi-ouvert, essai
        guard.recordFailure(TENANT); // l'essai echoue -> re-ouverture immediate

        assertThatThrownBy(() -> guard.check(ctx(10)))
                .isInstanceOf(AiCircuitOpenException.class);
    }

    @Test
    void disabledGuardIsNoOp() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T10:00:00Z"));
        AiGuardProperties p = props(1, 1, 10, 1, 30);
        p.setEnabled(false);
        TokenBucketAiGuard guard = new TokenBucketAiGuard(p, clock);

        // Au-dela de toutes les bornes, mais desactive -> aucun rejet.
        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> guard.check(ctx(9999))).doesNotThrowAnyException();
        }
    }

    @Test
    void quotasAreIsolatedPerTenant() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-31T10:00:00Z"));
        TokenBucketAiGuard guard = new TokenBucketAiGuard(props(100, 1, 1000, 5, 30), clock);

        guard.check(new AiCallContext("tenant-a", "complete", 10));
        assertThatThrownBy(() -> guard.check(new AiCallContext("tenant-a", "complete", 10)))
                .isInstanceOf(AiQuotaExceededException.class);
        // tenant-b dispose de son propre quota.
        assertThatCode(() -> guard.check(new AiCallContext("tenant-b", "complete", 10)))
                .doesNotThrowAnyException();
    }

    /** Horloge mutable pour piloter le temps dans les tests. */
    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant start) {
            this(start, ZoneOffset.UTC);
        }

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void advance(Duration d) {
            this.instant = this.instant.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId z) {
            return new MutableClock(instant, z);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
