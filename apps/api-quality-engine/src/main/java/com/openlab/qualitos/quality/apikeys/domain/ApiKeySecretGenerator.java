package com.openlab.qualitos.quality.apikeys.domain;

/**
 * Port — génère une paire (prefix public, secret en clair). Implémentation infra
 * utilise SecureRandom + base64url. Format de la clé en clair présentée au client :
 * {@code qos_<prefix>_<secret>}.
 *
 * Sécurité : le secret en clair doit avoir ≥ 256 bits d'entropie effective
 * (32 octets aléatoires base64url-encoded ≈ 43 chars).
 */
public interface ApiKeySecretGenerator {

    record Material(String prefix, String rawSecret, String plaintextRepresentation) {}

    Material generate();
}
