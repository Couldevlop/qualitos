package com.openlab.qualitos.quality.consent.domain;

/**
 * Port — hash déterministe d'un identifiant de sujet (email, identifiant…).
 * Implémenté en infrastructure (SHA-256 par défaut).
 *
 * Pourquoi un port local au bounded context consent et pas réutilisation de
 * gdpr.domain.SubjectIdentifierHasher : Clean Architecture — chaque bounded
 * context possède ses propres ports. L'implémentation concrète peut être
 * partagée à l'échelle infrastructure (cf. {@code Sha256SubjectIdentifierHasher}).
 */
public interface SubjectIdentifierHasher {
    String hash(String rawIdentifier);
}
