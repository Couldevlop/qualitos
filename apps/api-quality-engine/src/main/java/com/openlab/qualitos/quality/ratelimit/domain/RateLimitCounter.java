package com.openlab.qualitos.quality.ratelimit.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Port — compteur sliding/fixed window. L'implémentation infra peut être
 * in-memory (V1) ou Redis (multi-instance, V2).
 *
 * Contrat de {@link #incrementAndGet} : opération atomique. Renvoie le compteur
 * APRÈS incrément.
 */
public interface RateLimitCounter {

    /**
     * Incrémente le compteur pour (tenantId, scope) sur la fenêtre fixe couvrant
     * {@code now}. Renvoie l'état post-incrément.
     *
     * @return ({@code count}, {@code windowEnd})
     */
    Snapshot incrementAndGet(UUID tenantId, String scope, int windowSeconds, Instant now);

    /** Lecture sans incrément (admin). */
    Snapshot peek(UUID tenantId, String scope, int windowSeconds, Instant now);

    record Snapshot(int count, Instant windowEnd) {}
}
