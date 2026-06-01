package com.openlab.qualitos.quality.ai.guard;

/**
 * Débit d'appels IA dépassé pour le tenant (token bucket vide).
 * Mappé en HTTP 429 (avec en-tête {@code Retry-After}). OWASP LLM04.
 */
public class AiRateLimitExceededException extends AiGuardException {

    private final long retryAfterSeconds;

    public AiRateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
