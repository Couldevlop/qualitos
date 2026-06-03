package com.openlab.qualitos.crypto.infrastructure;

import com.openlab.qualitos.crypto.domain.CryptoException;
import com.openlab.qualitos.crypto.domain.model.KemAlgorithm;
import com.openlab.qualitos.crypto.domain.port.KemProvider;

import javax.crypto.KEM;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Real ML-KEM (FIPS 203) key encapsulation backed by the <b>standard JCA</b> :
 * {@link KeyPairGenerator}/{@link KeyFactory} {@code "ML-KEM"} + {@link javax.crypto.KEM}
 * (JDK 21, JEP 452). Le provider Bouncy Castle est obtenu dynamiquement
 * ({@link BcProviderRegistrar}) — donc le MÊME code marche en {@code bcprov} (« BC ») et
 * en {@code bc-fips} (« BCFIPS »), ce dernier n'exposant QUE l'API JCA (frontière FIPS) et
 * PAS l'API bas-niveau {@code org.bouncycastle.pqc.crypto.*} utilisée auparavant (ADR 0011,
 * crypto-agility).
 *
 * <p>Sérialisation (impl-specific, auto-descriptive — inchangée vs l'impl bas-niveau) :
 * <ul>
 *   <li>{@code generateKeyPair()} → {@code [int pubLen][pub(X.509)][priv(PKCS#8)]}</li>
 *   <li>{@code encapsulate(pub)} → {@code [int secretLen][secret][ciphertext]}</li>
 * </ul>
 * Les clés voyagent encodées (X.509 SubjectPublicKeyInfo / PKCS#8) — l'encodage porte le
 * jeu de paramètres ML-KEM, donc un {@code KeyFactory("ML-KEM")} générique les reconstruit.
 */
public final class BouncyCastleKemProvider implements KemProvider {

  private static final String BC = BcProviderRegistrar.ensureRegistered();
  private static final String ALG = "ML-KEM";

  @Override
  public KemAlgorithm algorithm() {
    return KemAlgorithm.ML_KEM_768;
  }

  @Override
  public byte[] generateKeyPair() {
    try {
      // Nom d'algorithme portant le paramètre (ML-KEM-768) plutôt qu'une classe spec BC
      // (MLKEMParameterSpec absente sous ce package en bc-fips) → portable JCA.
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-KEM-768", BC);
      KeyPair kp = kpg.generateKeyPair();
      return pack(kp.getPublic().getEncoded(), kp.getPrivate().getEncoded());
    } catch (GeneralSecurityException e) {
      throw new CryptoException("ML-KEM key generation failed", e);
    }
  }

  @Override
  public byte[] encapsulate(byte[] publicKey) {
    try {
      PublicKey pub = KeyFactory.getInstance(ALG, BC)
          .generatePublic(new X509EncodedKeySpec(publicKey));
      KEM.Encapsulator enc = KEM.getInstance(ALG, BC).newEncapsulator(pub);
      KEM.Encapsulated e = enc.encapsulate();
      return pack(e.key().getEncoded(), e.encapsulation());
    } catch (GeneralSecurityException e) {
      throw new CryptoException("ML-KEM encapsulation failed", e);
    }
  }

  @Override
  public byte[] decapsulate(byte[] privateKey, byte[] ciphertext) {
    try {
      PrivateKey priv = KeyFactory.getInstance(ALG, BC)
          .generatePrivate(new PKCS8EncodedKeySpec(privateKey));
      KEM.Decapsulator dec = KEM.getInstance(ALG, BC).newDecapsulator(priv);
      SecretKey secret = dec.decapsulate(ciphertext);
      return secret.getEncoded();
    } catch (GeneralSecurityException e) {
      throw new CryptoException("ML-KEM decapsulation failed", e);
    }
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
