package com.openlab.qualitos.crypto.domain.model;

/**
 * Signature algorithm — classical and post-quantum (CLAUDE.md §11.4).
 *
 * <p>ML-DSA (Module-Lattice Digital Signature Algorithm) is FIPS 204 standard.
 * Hybrid signing combines Ed25519 + ML-DSA-65 for audit reports + blockchain
 * blocks so the validity outlives the lifetime of the Ed25519 curve.
 */
public enum SignatureAlgorithm {
  ED25519("Ed25519", false, 64),
  ML_DSA_44("ML-DSA-44", true, 2420),
  ML_DSA_65("ML-DSA-65", true, 3309),
  ML_DSA_87("ML-DSA-87", true, 4595);

  private final String oid;
  private final boolean postQuantum;
  private final int signatureBytes;

  SignatureAlgorithm(String oid, boolean postQuantum, int signatureBytes) {
    this.oid = oid;
    this.postQuantum = postQuantum;
    this.signatureBytes = signatureBytes;
  }

  public String oid() { return oid; }
  public boolean postQuantum() { return postQuantum; }
  public int signatureBytes() { return signatureBytes; }
}
