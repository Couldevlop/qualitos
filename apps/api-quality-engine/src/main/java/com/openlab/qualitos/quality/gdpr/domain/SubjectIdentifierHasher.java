package com.openlab.qualitos.quality.gdpr.domain;

/**
 * Port — hash de l'identifiant de la personne concernée (email, n° client…)
 * pour ne PAS stocker la PII en clair (OWASP A02 / RGPD minimisation).
 * Impl infra : SHA-256 hex.
 */
public interface SubjectIdentifierHasher {
    String hash(String rawIdentifier);
}
