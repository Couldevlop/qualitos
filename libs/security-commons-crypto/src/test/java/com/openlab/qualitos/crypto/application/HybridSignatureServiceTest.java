package com.openlab.qualitos.crypto.application;

import com.openlab.qualitos.crypto.domain.CryptoException;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureEnvelope;
import com.openlab.qualitos.crypto.domain.port.SignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.BouncyCastleSignatureProvider;
import com.openlab.qualitos.crypto.infrastructure.InMemorySigningKeyProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HybridSignatureServiceTest {

  private static final byte[] ROOT =
      "merkle-root-deadbeef".getBytes(StandardCharsets.UTF_8);

  private HybridSignatureService service;

  @BeforeEach
  void setUp() {
    List<SignatureProvider> providers = List.of(
        new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519),
        new BouncyCastleSignatureProvider(SignatureAlgorithm.ML_DSA_65));
    InMemorySigningKeyProvider keys = new InMemorySigningKeyProvider(
        "platform-v1",
        algo -> new BouncyCastleSignatureProvider(algo).generateKeyPair());
    service = new HybridSignatureService(
        new CryptoSuiteConfig(), providers, keys,
        Clock.fixed(Instant.parse("2026-05-26T10:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void signProducesHybridTwoPartEnvelope() {
    SignatureEnvelope env = service.sign("blockchain-block", ROOT);

    assertThat(env.suiteName()).isEqualTo("blockchain-block");
    assertThat(env.keyRef()).isEqualTo("platform-v1");
    assertThat(env.parts()).extracting(SignatureEnvelope.Part::algorithm)
        .containsExactly(SignatureAlgorithm.ED25519, SignatureAlgorithm.ML_DSA_65);
  }

  @Test
  void verifyAcceptsGenuineEnvelope() {
    SignatureEnvelope env = service.sign("audit-report", ROOT);

    assertThat(service.verify(ROOT, env)).isTrue();
  }

  @Test
  void verifySurvivesEncodeDecodeRoundTrip() {
    SignatureEnvelope env = service.sign("audit-report", ROOT);

    SignatureEnvelope restored = SignatureEnvelope.decode(env.encode());

    assertThat(service.verify(ROOT, restored)).isTrue();
  }

  @Test
  void verifyRejectsTamperedMessage() {
    SignatureEnvelope env = service.sign("blockchain-block", ROOT);

    byte[] tampered = "merkle-root-deadbee0".getBytes(StandardCharsets.UTF_8);

    assertThat(service.verify(tampered, env)).isFalse();
  }

  @Test
  void verifyRejectsForgedPublicKey() {
    SignatureEnvelope genuine = service.sign("blockchain-block", ROOT);

    // Attacker re-signs the message with their OWN key and embeds their pubkey.
    BouncyCastleSignatureProvider attackerEd =
        new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519);
    var attackerKey = attackerEd.generateKeyPair();
    byte[] forgedSig = attackerEd.sign(attackerKey.privateKey(), ROOT);

    SignatureEnvelope forged = new SignatureEnvelope(
        genuine.version(), genuine.suiteName(), genuine.keyRef(), genuine.signedAt(),
        List.of(new SignatureEnvelope.Part(
            SignatureAlgorithm.ED25519, attackerKey.publicKey(), forgedSig)));

    // Key pinning: embedded pubkey != trusted platform pubkey → rejected.
    assertThat(service.verify(ROOT, forged)).isFalse();
  }

  @Test
  void verifyRejectsUnknownKeyRef() {
    SignatureEnvelope env = service.sign("audit-report", ROOT);
    SignatureEnvelope wrongRef = new SignatureEnvelope(
        env.version(), env.suiteName(), "platform-v99", env.signedAt(), env.parts());

    assertThat(service.verify(ROOT, wrongRef)).isFalse();
  }

  @Test
  void signUnknownContextThrows() {
    assertThatThrownBy(() -> service.sign("does-not-exist", ROOT))
        .isInstanceOf(CryptoException.class);
  }

  @Test
  void verifyRejectsEnvelopeWithUnregisteredAlgorithmProvider() {
    SignatureEnvelope env = service.sign("blockchain-block", ROOT);

    // A service that only knows Ed25519 cannot verify the ML-DSA-65 part.
    InMemorySigningKeyProvider keys = new InMemorySigningKeyProvider(
        "platform-v1", algo -> new BouncyCastleSignatureProvider(algo).generateKeyPair());
    HybridSignatureService ed25519Only = new HybridSignatureService(
        new CryptoSuiteConfig(),
        List.of(new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519)),
        keys, Clock.systemUTC());

    assertThat(ed25519Only.verify(ROOT, env)).isFalse();
  }

  @Test
  void signThrowsWhenSuiteAlgorithmHasNoProvider() {
    // Service missing the ML-DSA-65 provider required by the blockchain-block suite.
    InMemorySigningKeyProvider keys = new InMemorySigningKeyProvider(
        "platform-v1", algo -> new BouncyCastleSignatureProvider(algo).generateKeyPair());
    HybridSignatureService incomplete = new HybridSignatureService(
        new CryptoSuiteConfig(),
        List.of(new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519)),
        keys, Clock.systemUTC());

    assertThatThrownBy(() -> incomplete.sign("blockchain-block", ROOT))
        .isInstanceOf(CryptoException.class);
  }
}
