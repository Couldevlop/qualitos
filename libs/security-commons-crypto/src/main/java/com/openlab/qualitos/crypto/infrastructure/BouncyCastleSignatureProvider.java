package com.openlab.qualitos.crypto.infrastructure;

import com.openlab.qualitos.crypto.domain.CryptoException;
import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.port.SignatureProvider;
import org.bouncycastle.jcajce.spec.MLDSAParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Real signature primitive backed by Bouncy Castle (ADR 0011 §2).
 *
 * <p>One instance handles exactly one {@link SignatureAlgorithm}:
 * <ul>
 *   <li>{@code ED25519} via the JDK provider;</li>
 *   <li>{@code ML_DSA_44/65/87} (FIPS 204) via the {@code BC} provider.</li>
 * </ul>
 *
 * <p>Keys cross the SPI as encoded bytes (X.509 {@code SubjectPublicKeyInfo} for
 * the public key, PKCS#8 for the private key) — the encoded form carries the
 * ML-DSA parameter set, so a generic {@code "ML-DSA"} {@link KeyFactory} can
 * reconstruct them. Crypto-agility: swapping to {@code bc-fips} keeps this SPI.
 */
public final class BouncyCastleSignatureProvider implements SignatureProvider {

  private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

  static {
    if (Security.getProvider(BC) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  private final SignatureAlgorithm algorithm;

  public BouncyCastleSignatureProvider(SignatureAlgorithm algorithm) {
    if (algorithm == null) {
      throw new IllegalArgumentException("algorithm required");
    }
    this.algorithm = algorithm;
  }

  @Override
  public SignatureAlgorithm algorithm() {
    return algorithm;
  }

  /** Generate a fresh key pair for this algorithm (used by SigningKeyProvider). */
  public KeyMaterial generateKeyPair() {
    try {
      KeyPairGenerator kpg;
      if (algorithm == SignatureAlgorithm.ED25519) {
        kpg = KeyPairGenerator.getInstance("Ed25519");
      } else {
        kpg = KeyPairGenerator.getInstance("ML-DSA", BC);
        kpg.initialize(mldsaSpec(), new SecureRandom());
      }
      KeyPair kp = kpg.generateKeyPair();
      return new KeyMaterial(kp.getPublic().getEncoded(), kp.getPrivate().getEncoded());
    } catch (GeneralSecurityException e) {
      throw new CryptoException("key generation failed for " + algorithm, e);
    }
  }

  @Override
  public byte[] sign(byte[] privateKey, byte[] message) {
    try {
      PrivateKey key = keyFactory().generatePrivate(new PKCS8EncodedKeySpec(privateKey));
      Signature signer = signature();
      signer.initSign(key);
      signer.update(message);
      return signer.sign();
    } catch (GeneralSecurityException e) {
      throw new CryptoException("signing failed for " + algorithm, e);
    }
  }

  @Override
  public boolean verify(byte[] publicKey, byte[] message, byte[] signature) {
    PublicKey key;
    try {
      key = keyFactory().generatePublic(new X509EncodedKeySpec(publicKey));
    } catch (GeneralSecurityException e) {
      throw new CryptoException("invalid public key for " + algorithm, e);
    }
    try {
      Signature verifier = signature();
      verifier.initVerify(key);
      verifier.update(message);
      return verifier.verify(signature);
    } catch (SignatureException e) {
      // malformed / wrong signature → verification simply fails
      return false;
    } catch (GeneralSecurityException e) {
      throw new CryptoException("verification error for " + algorithm, e);
    }
  }

  private KeyFactory keyFactory() throws GeneralSecurityException {
    return algorithm == SignatureAlgorithm.ED25519
        ? KeyFactory.getInstance("Ed25519")
        : KeyFactory.getInstance("ML-DSA", BC);
  }

  private Signature signature() throws GeneralSecurityException {
    return algorithm == SignatureAlgorithm.ED25519
        ? Signature.getInstance("Ed25519")
        : Signature.getInstance("ML-DSA", BC);
  }

  private AlgorithmParameterSpec mldsaSpec() {
    return switch (algorithm) {
      case ML_DSA_44 -> MLDSAParameterSpec.ml_dsa_44;
      case ML_DSA_65 -> MLDSAParameterSpec.ml_dsa_65;
      case ML_DSA_87 -> MLDSAParameterSpec.ml_dsa_87;
      default -> throw new CryptoException("not an ML-DSA algorithm: " + algorithm);
    };
  }
}
