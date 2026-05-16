package com.openlab.qualitos.quality.ratelimit.domain;

/**
 * Décision d'un check de rate-limit. {@code allowed=false} ⇒ le caller doit
 * rejeter la requête avec HTTP 429 (Retry-After: retryAfterSeconds).
 *
 * Champs additionnels (headers RFC 6585) :
 *  - {@code limit}        : max requêtes sur la fenêtre
 *  - {@code remaining}    : restant (≥ 0)
 *  - {@code resetSeconds} : secondes avant reset complet
 */
public record RateLimitDecision(
        boolean allowed,
        int limit,
        int remaining,
        int retryAfterSeconds,
        int resetSeconds
) {
    public static RateLimitDecision allow(int limit, int remaining, int resetSeconds) {
        return new RateLimitDecision(true, limit, Math.max(0, remaining), 0, resetSeconds);
    }

    public static RateLimitDecision deny(int limit, int retryAfterSeconds, int resetSeconds) {
        return new RateLimitDecision(false, limit, 0, retryAfterSeconds, resetSeconds);
    }

    /** Aucune politique configurée pour ce scope → ouvert. */
    public static RateLimitDecision unlimited() {
        return new RateLimitDecision(true, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
    }
}
