package com.openlab.qualitos.crypto.domain.port;

import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;

/** SPI for digital signature primitives (Ed25519 / ML-DSA). */
public interface SignatureProvider {
  SignatureAlgorithm algorithm();

  byte[] sign(byte[] privateKey, byte[] message);
  boolean verify(byte[] publicKey, byte[] message, byte[] signature);
}
