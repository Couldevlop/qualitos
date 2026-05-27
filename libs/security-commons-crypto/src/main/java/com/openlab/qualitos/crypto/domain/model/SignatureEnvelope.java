package com.openlab.qualitos.crypto.domain.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Versioned, self-describing signature envelope (ADR 0011).
 *
 * <p>Holds one signature {@link Part} per algorithm of a hybrid suite (e.g.
 * Ed25519 + ML-DSA-65). Verification requires <b>all</b> parts to validate, so
 * the guarantee outlives the rupture of any single algorithm.
 *
 * <p>Encoded as a length-prefixed binary blob, then Base64URL (no padding), so
 * it can live in a {@code varchar} column or a QR code.
 */
public record SignatureEnvelope(
    int version,
    String suiteName,
    String keyRef,
    Instant signedAt,
    List<Part> parts) {

  public static final int CURRENT_VERSION = 1;

  public SignatureEnvelope {
    Objects.requireNonNull(suiteName, "suiteName");
    Objects.requireNonNull(keyRef, "keyRef");
    Objects.requireNonNull(signedAt, "signedAt");
    if (parts == null || parts.isEmpty()) {
      throw new IllegalArgumentException("at least one signature part required");
    }
    parts = List.copyOf(parts);
  }

  /** One algorithm's contribution: its public key + signature over the message. */
  public record Part(SignatureAlgorithm algorithm, byte[] publicKey, byte[] signature) {
    public Part {
      Objects.requireNonNull(algorithm, "algorithm");
      if (publicKey == null || publicKey.length == 0) {
        throw new IllegalArgumentException("publicKey required");
      }
      if (signature == null || signature.length == 0) {
        throw new IllegalArgumentException("signature required");
      }
    }
  }

  /** Serialize to a Base64URL (unpadded) string. */
  public String encode() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bos)) {
      out.writeByte(version);
      out.writeUTF(suiteName);
      out.writeUTF(keyRef);
      out.writeLong(signedAt.toEpochMilli());
      out.writeByte(parts.size());
      for (Part p : parts) {
        out.writeUTF(p.algorithm().name());
        out.writeInt(p.publicKey().length);
        out.write(p.publicKey());
        out.writeInt(p.signature().length);
        out.write(p.signature());
      }
    } catch (IOException e) {
      throw new UncheckedIOException("envelope encode failed", e);
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
  }

  /** Parse a Base64URL (unpadded) string back into an envelope. */
  public static SignatureEnvelope decode(String encoded) {
    Objects.requireNonNull(encoded, "encoded");
    byte[] raw = Base64.getUrlDecoder().decode(encoded);
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
      int version = in.readUnsignedByte();
      String suiteName = in.readUTF();
      String keyRef = in.readUTF();
      Instant signedAt = Instant.ofEpochMilli(in.readLong());
      int n = in.readUnsignedByte();
      List<Part> parts = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        SignatureAlgorithm algo = SignatureAlgorithm.valueOf(in.readUTF());
        byte[] pub = in.readNBytes(in.readInt());
        byte[] sig = in.readNBytes(in.readInt());
        parts.add(new Part(algo, pub, sig));
      }
      return new SignatureEnvelope(version, suiteName, keyRef, signedAt, parts);
    } catch (IOException e) {
      throw new UncheckedIOException("envelope decode failed", e);
    }
  }
}
