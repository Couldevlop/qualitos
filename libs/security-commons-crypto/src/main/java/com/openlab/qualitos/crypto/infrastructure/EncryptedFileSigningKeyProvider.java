package com.openlab.qualitos.crypto.infrastructure;

import com.openlab.qualitos.crypto.domain.CryptoException;
import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.port.SigningKeyProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Dev/on-prem platform key store (ADR 0011 §4): one AES-256-GCM-encrypted file
 * per algorithm under {@code dir}. The AES key is supplied at construction
 * (sourced from an env var by the Spring wiring — <b>never a clear secret on
 * disk</b>, CLAUDE.md §18.2-3). Keys are generated on first use and reused.
 *
 * <p>Single key version per directory (rotation = new directory + keyRef). Prod
 * swaps this adapter for a Vault Transit one without touching consumers.
 */
public final class EncryptedFileSigningKeyProvider implements SigningKeyProvider {

  private static final int IV_LEN = 12;
  private static final int TAG_BITS = 128;

  private final Path dir;
  private final byte[] aesKey;
  private final String keyRef;
  private final Function<SignatureAlgorithm, KeyMaterial> keyGenerator;
  private final SecureRandom random = new SecureRandom();
  private final ConcurrentHashMap<SignatureAlgorithm, KeyMaterial> cache = new ConcurrentHashMap<>();

  public EncryptedFileSigningKeyProvider(Path dir,
                                         byte[] aesKey,
                                         String keyRef,
                                         Function<SignatureAlgorithm, KeyMaterial> keyGenerator) {
    this.dir = Objects.requireNonNull(dir, "dir");
    this.keyRef = Objects.requireNonNull(keyRef, "keyRef");
    this.keyGenerator = Objects.requireNonNull(keyGenerator, "keyGenerator");
    if (aesKey == null || aesKey.length != 32) {
      throw new IllegalArgumentException("aesKey must be 32 bytes (AES-256)");
    }
    this.aesKey = aesKey.clone();
  }

  @Override
  public String currentKeyRef(SignatureAlgorithm algorithm) {
    return keyRef;
  }

  @Override
  public synchronized KeyMaterial signingKey(SignatureAlgorithm algorithm) {
    return cache.computeIfAbsent(algorithm, this::loadOrCreate);
  }

  @Override
  public byte[] trustedPublicKey(SignatureAlgorithm algorithm, String keyRef) {
    if (!this.keyRef.equals(keyRef)) {
      return null;
    }
    return signingKey(algorithm).publicKey();
  }

  private KeyMaterial loadOrCreate(SignatureAlgorithm algorithm) {
    Path file = dir.resolve(algorithm.name() + ".key.enc");
    try {
      if (Files.exists(file)) {
        return deserialize(decrypt(Files.readAllBytes(file)));
      }
      KeyMaterial generated = keyGenerator.apply(algorithm);
      Files.createDirectories(dir);
      Files.write(file, encrypt(serialize(generated)));
      return generated;
    } catch (IOException e) {
      throw new UncheckedIOException("key store I/O failed for " + algorithm, e);
    }
  }

  private byte[] encrypt(byte[] plaintext) {
    try {
      byte[] iv = new byte[IV_LEN];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"),
          new GCMParameterSpec(TAG_BITS, iv));
      byte[] ct = cipher.doFinal(plaintext);
      byte[] out = new byte[IV_LEN + ct.length];
      System.arraycopy(iv, 0, out, 0, IV_LEN);
      System.arraycopy(ct, 0, out, IV_LEN, ct.length);
      return out;
    } catch (GeneralSecurityException e) {
      throw new CryptoException("key encryption failed", e);
    }
  }

  private byte[] decrypt(byte[] stored) {
    try {
      byte[] iv = new byte[IV_LEN];
      System.arraycopy(stored, 0, iv, 0, IV_LEN);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"),
          new GCMParameterSpec(TAG_BITS, iv));
      return cipher.doFinal(stored, IV_LEN, stored.length - IV_LEN);
    } catch (GeneralSecurityException e) {
      throw new CryptoException("key decryption failed (wrong AES key or tampered file)", e);
    }
  }

  private static byte[] serialize(KeyMaterial km) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bos)) {
      out.writeInt(km.publicKey().length);
      out.write(km.publicKey());
      out.writeInt(km.privateKey().length);
      out.write(km.privateKey());
    } catch (IOException e) {
      throw new UncheckedIOException("key serialize failed", e);
    }
    return bos.toByteArray();
  }

  private static KeyMaterial deserialize(byte[] bytes) {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
      byte[] pub = in.readNBytes(in.readInt());
      byte[] priv = in.readNBytes(in.readInt());
      return new KeyMaterial(pub, priv);
    } catch (IOException e) {
      throw new UncheckedIOException("key deserialize failed", e);
    }
  }
}
