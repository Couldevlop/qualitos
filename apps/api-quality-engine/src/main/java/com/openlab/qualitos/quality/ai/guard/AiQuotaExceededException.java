package com.openlab.qualitos.quality.ai.guard;

/**
 * Quota journalier d'appels IA épuisé pour le tenant.
 * Mappé en HTTP 429. OWASP LLM04 (Model DoS) — limite de consommation par tenant.
 */
public class AiQuotaExceededException extends AiGuardException {

    public AiQuotaExceededException(String message) {
        super(message);
    }
}
