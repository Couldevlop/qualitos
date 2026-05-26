package com.openlab.qualitos.crypto.domain.port;

import com.openlab.qualitos.crypto.domain.model.KemAlgorithm;

/**
 * SPI for KEM (Key Encapsulation Mechanism) primitives.
 *
 * <p>Production implementation: BouncyCastleKemProvider (BC with ML-KEM / FIPS 203).
 */
public interface KemProvider {
  KemAlgorithm algorithm();

  byte[] generateKeyPair();   // serialized form (impl-specific)
  byte[] encapsulate(byte[] publicKey);   // returns shared secret || ciphertext
  byte[] decapsulate(byte[] privateKey, byte[] ciphertext);
}
