package com.openlab.qualitos.quality.ai.guard;

/**
 * Disjoncteur ouvert : le service IA a enchaîné les échecs, les appels sont
 * court-circuités le temps du refroidissement pour ne pas l'enfoncer davantage
 * (fail-fast). Mappé en HTTP 503. OWASP LLM04.
 */
public class AiCircuitOpenException extends AiGuardException {

    private final long retryAfterSeconds;

    public AiCircuitOpenException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
