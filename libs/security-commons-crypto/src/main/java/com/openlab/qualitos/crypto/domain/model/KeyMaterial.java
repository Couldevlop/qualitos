package com.openlab.qualitos.crypto.domain.model;

/**
 * Raw key pair bytes for one signature algorithm (impl-specific encoding —
 * X.509 SubjectPublicKeyInfo + PKCS#8 for the BouncyCastle JCA provider).
 *
 * <p>Carries no algorithm tag on purpose: it is always paired with its
 * {@link SignatureAlgorithm} at the call site.
 */
public record KeyMaterial(byte[] publicKey, byte[] privateKey) {

  public KeyMaterial {
    if (publicKey == null || publicKey.length == 0) {
      throw new IllegalArgumentException("publicKey required");
    }
    if (privateKey == null || privateKey.length == 0) {
      throw new IllegalArgumentException("privateKey required");
    }
  }
}
