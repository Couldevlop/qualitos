package com.openlab.qualitos.crypto.infrastructure;

import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.port.SigningKeyProvider;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Ephemeral, in-memory platform keys (ADR 0011 §4). Generates one key pair per
 * algorithm on first use and keeps it for the process lifetime.
 *
 * <p>Intended for tests and ephemeral/dev runs. Persistent dev keys use
 * {@link EncryptedFileSigningKeyProvider}; prod uses Vault Transit.
 */
public final class InMemorySigningKeyProvider implements SigningKeyProvider {

  private final String keyRef;
  private final Function<SignatureAlgorithm, KeyMaterial> keyGenerator;
  private final Map<SignatureAlgorithm, KeyMaterial> cache = new ConcurrentHashMap<>();

  public InMemorySigningKeyProvider(String keyRef,
                                    Function<SignatureAlgorithm, KeyMaterial> keyGenerator) {
    this.keyRef = Objects.requireNonNull(keyRef, "keyRef");
    this.keyGenerator = Objects.requireNonNull(keyGenerator, "keyGenerator");
  }

  @Override
  public String currentKeyRef(SignatureAlgorithm algorithm) {
    return keyRef;
  }

  @Override
  public KeyMaterial signingKey(SignatureAlgorithm algorithm) {
    return cache.computeIfAbsent(algorithm, keyGenerator);
  }

  @Override
  public byte[] trustedPublicKey(SignatureAlgorithm algorithm, String keyRef) {
    if (!this.keyRef.equals(keyRef)) {
      return null;
    }
    return signingKey(algorithm).publicKey();
  }
}
