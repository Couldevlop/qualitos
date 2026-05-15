package com.openlab.qualitos.quality.webhooks;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Politique de backoff exponentiel pour les retries webhook.
 *
 * Attempt 1 → 30s, 2 → 2min, 3 → 10min, 4 → 1h, 5 → 6h.
 * Plafond à 24h.
 */
@Component
public class RetryBackoff {

    /** Delais pour les essais 1..N. Doit avoir au moins maxRetries entrees. */
    static final long[] DELAYS_SECONDS = {
            30,          // 30s
            120,         // 2min
            600,         // 10min
            3600,        // 1h
            21600,       // 6h
            86400        // 24h
    };

    /**
     * @param attemptCount nombre d'essais déjà effectués (≥ 1 après le 1er fail)
     * @return instant du prochain essai
     */
    public Instant nextRetryAt(int attemptCount) {
        if (attemptCount < 1) {
            attemptCount = 1;
        }
        int idx = Math.min(attemptCount - 1, DELAYS_SECONDS.length - 1);
        return Instant.now().plus(Duration.ofSeconds(DELAYS_SECONDS[idx]));
    }
}
