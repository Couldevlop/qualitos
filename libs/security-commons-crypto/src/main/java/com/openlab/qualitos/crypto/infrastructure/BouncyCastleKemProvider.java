package com.openlab.qualitos.crypto.infrastructure;

import com.openlab.qualitos.crypto.domain.CryptoException;
import com.openlab.qualitos.crypto.domain.model.KemAlgorithm;
import com.openlab.qualitos.crypto.domain.port.KemProvider;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;

/**
 * Real ML-KEM (FIPS 203) key encapsulation backed by Bouncy Castle's low-level
 * PQC API (ADR 0011 §2). Used by the hybrid TLS suite (WS5, deferred) — shipped
 * now so the KEM half of crypto-agility is real and tested.
 *
 * <p>Serialization (impl-specific, self-describing):
 * <ul>
 *   <li>{@code generateKeyPair()} → {@code [int pubLen][pub][priv]}</li>
 *   <li>{@code encapsulate(pub)} → {@code [int secretLen][secret][ciphertext]}</li>
 * </ul>
 */
public final class BouncyCastleKemProvider implements KemProvider {

  private final SecureRandom random = new SecureRandom();

  @Override
  public KemAlgorithm algorithm() {
    return KemAlgorithm.ML_KEM_768;
  }

  @Override
  public byte[] generateKeyPair() {
    MLKEMKeyPairGenerator generator = new MLKEMKeyPairGenerator();
    generator.init(new MLKEMKeyGenerationParameters(random, MLKEMParameters.ml_kem_768));
    AsymmetricCipherKeyPair kp = generator.generateKeyPair();
    byte[] pub = ((MLKEMPublicKeyParameters) kp.getPublic()).getEncoded();
    byte[] priv = ((MLKEMPrivateKeyParameters) kp.getPrivate()).getEncoded();
    return pack(pub, priv);
  }

  @Override
  public byte[] encapsulate(byte[] publicKey) {
    MLKEMPublicKeyParameters pub =
        new MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, publicKey);
    SecretWithEncapsulation swe = new MLKEMGenerator(random).generateEncapsulated(pub);
    try {
      return pack(swe.getSecret(), swe.getEncapsulation());
    } finally {
      try {
        swe.destroy();
      } catch (Exception ignored) {
        // best effort zeroisation
      }
    }
  }

  @Override
  public byte[] decapsulate(byte[] privateKey, byte[] ciphertext) {
    MLKEMPrivateKeyParameters priv =
        new MLKEMPrivateKeyParameters(MLKEMParameters.ml_kem_768, privateKey);
    return new MLKEMExtractor(priv).extractSecret(ciphertext);
  }

  /** First component of a {@link #generateKeyPair()} blob (public key bytes). */
  public static byte[] publicKeyOf(byte[] keyPairBlob) {
    return first(keyPairBlob);
  }

  /** Second component of a {@link #generateKeyPair()} blob (private key bytes). */
  public static byte[] privateKeyOf(byte[] keyPairBlob) {
    return second(keyPairBlob);
  }

  /** First component of an {@link #encapsulate(byte[])} blob (shared secret). */
  public static byte[] sharedSecretOf(byte[] encapsulateBlob) {
    return first(encapsulateBlob);
  }

  /** Second component of an {@link #encapsulate(byte[])} blob (ciphertext). */
  public static byte[] ciphertextOf(byte[] encapsulateBlob) {
    return second(encapsulateBlob);
  }

  private static byte[] pack(byte[] a, byte[] b) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bos)) {
      out.writeInt(a.length);
      out.write(a);
      out.write(b);
    } catch (IOException e) {
      throw new UncheckedIOException("kem pack failed", e);
    }
    return bos.toByteArray();
  }

  private static byte[] first(byte[] blob) {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(blob))) {
      return in.readNBytes(in.readInt());
    } catch (IOException e) {
      throw new CryptoException("kem blob unpack failed", e);
    }
  }

  private static byte[] second(byte[] blob) {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(blob))) {
      int aLen = in.readInt();
      in.skipNBytes(aLen);
      return in.readAllBytes();
    } catch (IOException e) {
      throw new CryptoException("kem blob unpack failed", e);
    }
  }
}
