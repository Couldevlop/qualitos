package com.openlab.qualitos.crypto.infrastructure;

import com.openlab.qualitos.crypto.domain.CryptoException;
import com.openlab.qualitos.crypto.domain.model.KeyMaterial;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BouncyCastleSignatureProviderTest {

  private static final byte[] MESSAGE = "QualitOS audit anchor root".getBytes(StandardCharsets.UTF_8);

  @ParameterizedTest
  @EnumSource(SignatureAlgorithm.class)
  void signThenVerifyRoundTrips(SignatureAlgorithm algo) {
    BouncyCastleSignatureProvider provider = new BouncyCastleSignatureProvider(algo);
    KeyMaterial kp = provider.generateKeyPair();

    byte[] signature = provider.sign(kp.privateKey(), MESSAGE);

    assertThat(provider.algorithm()).isEqualTo(algo);
    assertThat(signature).isNotEmpty();
    assertThat(provider.verify(kp.publicKey(), MESSAGE, signature)).isTrue();
  }

  @Test
  void mlDsa65SignatureIsRealNotPlaceholder() {
    BouncyCastleSignatureProvider provider =
        new BouncyCastleSignatureProvider(SignatureAlgorithm.ML_DSA_65);
    KeyMaterial kp = provider.generateKeyPair();

    byte[] signature = provider.sign(kp.privateKey(), MESSAGE);

    // ML-DSA-65 signatures are several KB — proves this is a genuine PQ signature.
    assertThat(signature.length).isGreaterThan(2000);
  }

  @ParameterizedTest
  @EnumSource(SignatureAlgorithm.class)
  void tamperedMessageFailsVerification(SignatureAlgorithm algo) {
    BouncyCastleSignatureProvider provider = new BouncyCastleSignatureProvider(algo);
    KeyMaterial kp = provider.generateKeyPair();
    byte[] signature = provider.sign(kp.privateKey(), MESSAGE);

    byte[] tampered = "QualitOS audit anchor ROOT".getBytes(StandardCharsets.UTF_8);

    assertThat(provider.verify(kp.publicKey(), tampered, signature)).isFalse();
  }

  @ParameterizedTest
  @EnumSource(SignatureAlgorithm.class)
  void wrongKeyFailsVerification(SignatureAlgorithm algo) {
    BouncyCastleSignatureProvider provider = new BouncyCastleSignatureProvider(algo);
    KeyMaterial signer = provider.generateKeyPair();
    KeyMaterial other = provider.generateKeyPair();
    byte[] signature = provider.sign(signer.privateKey(), MESSAGE);

    assertThat(provider.verify(other.publicKey(), MESSAGE, signature)).isFalse();
  }

  @Test
  void corruptedSignatureFailsGracefully() {
    BouncyCastleSignatureProvider provider =
        new BouncyCastleSignatureProvider(SignatureAlgorithm.ED25519);
    KeyMaterial kp = provider.generateKeyPair();
    byte[] signature = provider.sign(kp.privateKey(), MESSAGE);
    signature[0] ^= 0x01;

    assertThat(provider.verify(kp.publicKey(), MESSAGE, signature)).isFalse();
  }

  @Test
  void invalidPublicKeyThrows() {
    BouncyCastleSignatureProvider provider =
        new BouncyCastleSignatureProvider(SignatureAlgorithm.ML_DSA_65);

    assertThatThrownBy(() -> provider.verify(new byte[]{1, 2, 3}, MESSAGE, new byte[]{4, 5, 6}))
        .isInstanceOf(CryptoException.class);
  }

  @Test
  void nullAlgorithmRejected() {
    assertThatThrownBy(() -> new BouncyCastleSignatureProvider(null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
