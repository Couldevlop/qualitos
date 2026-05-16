package com.openlab.qualitos.quality.apikeys.domain;

/**
 * Port — hash + verify d'une clé. Implémentation infra : bcrypt strength ≥ 12.
 * {@link #matches} DOIT être à temps constant (bcrypt l'est par construction).
 */
public interface ApiKeyHasher {
    String hash(String rawSecret);
    boolean matches(String rawSecret, String hashedSecret);
}
