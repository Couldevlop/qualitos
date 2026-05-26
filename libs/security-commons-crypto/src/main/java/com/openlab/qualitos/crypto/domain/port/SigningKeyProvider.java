package com.openlab.qualitos.crypto.domain.port;

import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;

/**
 * Source of the platform signing keys (ADR 0011 §4).
 *
 * <p>The signing key proves the platform's attestation of integrity (audit
 * reports, certification dossiers, blockchain anchor receipts). It is a
 * <b>platform-level</b> key, not a per-tenant key.
 *
 * <p>Dev adapter: {@code EncryptedFileSigningKeyProvider} (AES-256-GCM file,
 * unlocked by env var). Prod adapter: Vault Transit (deferred — port ready).
 * Key rotation is modelled by {@code keyRef}: a signature pins the ref of the
 * key that produced it so verification survives rotation.
 */
public interface SigningKeyProvider {

  /** Stable identifier of the currently active key for an algorithm (rotation). */
  String currentKeyRef(SignatureAlgorithm algorithm);

  /** Key pair used to <b>sign</b> with the given algorithm (current key). */
  KeyMaterial signingKey(SignatureAlgorithm algorithm);

  /**
   * Trusted public key for an algorithm at a given {@code keyRef}.
   * Used at verification time to pin against a key the platform actually owns
   * (an attacker embedding their own public key in an envelope must not pass).
   *
   * @return the trusted public key bytes, or {@code null} if the ref is unknown.
   */
  byte[] trustedPublicKey(SignatureAlgorithm algorithm, String keyRef);
}
