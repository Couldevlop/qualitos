package com.openlab.qualitos.crypto.domain.model;

/**
 * Key Encapsulation Mechanism algorithm — classical and post-quantum.
 *
 * <p>CLAUDE.md §11.4 — hybrid TLS combines a classical curve (X25519) with a
 * NIST post-quantum KEM (ML-KEM-768) so a transcript captured today cannot be
 * decrypted by a future cryptanalytically relevant quantum computer.
 */
public enum KemAlgorithm {
  X25519("X25519", false, 32),
  ML_KEM_512("ML-KEM-512", true, 32),
  ML_KEM_768("ML-KEM-768", true, 32),
  ML_KEM_1024("ML-KEM-1024", true, 32);

  private final String oid;
  private final boolean postQuantum;
  private final int sharedSecretBytes;

  KemAlgorithm(String oid, boolean postQuantum, int sharedSecretBytes) {
    this.oid = oid;
    this.postQuantum = postQuantum;
    this.sharedSecretBytes = sharedSecretBytes;
  }

  public String oid() { return oid; }
  public boolean postQuantum() { return postQuantum; }
  public int sharedSecretBytes() { return sharedSecretBytes; }
}
