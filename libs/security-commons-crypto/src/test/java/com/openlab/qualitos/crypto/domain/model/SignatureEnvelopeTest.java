package com.openlab.qualitos.crypto.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignatureEnvelopeTest {

  @Test
  void encodeThenDecodeIsLossless() {
    SignatureEnvelope env = new SignatureEnvelope(
        SignatureEnvelope.CURRENT_VERSION,
        "blockchain-block",
        "platform-v1",
        Instant.parse("2026-05-26T10:00:00Z"),
        List.of(
            new SignatureEnvelope.Part(SignatureAlgorithm.ED25519, new byte[]{1, 2, 3}, new byte[]{4, 5}),
            new SignatureEnvelope.Part(SignatureAlgorithm.ML_DSA_65, new byte[]{6, 7}, new byte[]{8, 9, 10})));

    SignatureEnvelope restored = SignatureEnvelope.decode(env.encode());

    assertThat(restored.version()).isEqualTo(env.version());
    assertThat(restored.suiteName()).isEqualTo("blockchain-block");
    assertThat(restored.keyRef()).isEqualTo("platform-v1");
    assertThat(restored.signedAt()).isEqualTo(env.signedAt());
    assertThat(restored.parts()).hasSize(2);
    assertThat(restored.parts().get(1).algorithm()).isEqualTo(SignatureAlgorithm.ML_DSA_65);
    assertThat(restored.parts().get(1).publicKey()).containsExactly(6, 7);
    assertThat(restored.parts().get(1).signature()).containsExactly(8, 9, 10);
  }

  @Test
  void encodedFormIsUrlSafe() {
    SignatureEnvelope env = new SignatureEnvelope(
        1, "audit-report", "platform-v1", Instant.now(),
        List.of(new SignatureEnvelope.Part(SignatureAlgorithm.ED25519, new byte[]{1}, new byte[]{2})));

    assertThat(env.encode()).doesNotContain("+", "/", "=");
  }

  @Test
  void emptyPartsRejected() {
    assertThatThrownBy(() -> new SignatureEnvelope(
        1, "audit-report", "platform-v1", Instant.now(), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void partRejectsEmptySignature() {
    assertThatThrownBy(() -> new SignatureEnvelope.Part(
        SignatureAlgorithm.ED25519, new byte[]{1}, new byte[]{}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void partRejectsNullAlgorithm() {
    assertThatThrownBy(() -> new SignatureEnvelope.Part(null, new byte[]{1}, new byte[]{2}))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void partRejectsNullPublicKey() {
    assertThatThrownBy(() -> new SignatureEnvelope.Part(
        SignatureAlgorithm.ED25519, null, new byte[]{2}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void partRejectsEmptyPublicKey() {
    assertThatThrownBy(() -> new SignatureEnvelope.Part(
        SignatureAlgorithm.ED25519, new byte[]{}, new byte[]{2}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void partRejectsNullSignature() {
    assertThatThrownBy(() -> new SignatureEnvelope.Part(
        SignatureAlgorithm.ED25519, new byte[]{1}, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void envelopeRejectsNullSuiteName() {
    assertThatThrownBy(() -> new SignatureEnvelope(
        1, null, "platform-v1", Instant.now(),
        List.of(new SignatureEnvelope.Part(SignatureAlgorithm.ED25519, new byte[]{1}, new byte[]{2}))))
        .isInstanceOf(NullPointerException.class);
  }
}
