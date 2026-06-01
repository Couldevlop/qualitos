package com.openlab.qualitos.quality.ai.guard;

/**
 * Rejet d'un appel IA par le garde-fou {@link AiGuard} (OWASP LLM04 — Model DoS).
 *
 * <p>Distincte de {@code AiGatewayException} (échec réseau/HTTP en aval) : ici la
 * requête est <b>refusée en amont</b>, sans départ réseau. Les sous-types portent
 * la sémantique HTTP (429 / 413 / 503) via {@link AiGuardExceptionHandler}.
 */
public abstract class AiGuardException extends RuntimeException {

    protected AiGuardException(String message) {
        super(message);
    }
}
