package com.openlab.qualitos.crypto.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoSuiteTest {

  @Test
  void postQuantumReadyWhenPrimaryKemIsPq() {
    CryptoSuite s = new CryptoSuite("a", KemAlgorithm.ML_KEM_768, null,
        SignatureAlgorithm.ED25519, null, List.of());
    assertThat(s.isPostQuantumReady()).isTrue();
    assertThat(s.isHybrid()).isFalse();
  }

  @Test
  void postQuantumReadyWhenHybridKemIsPq() {
    CryptoSuite s = new CryptoSuite("a", KemAlgorithm.X25519, KemAlgorithm.ML_KEM_768,
        SignatureAlgorithm.ED25519, null, List.of());
    assertThat(s.isPostQuantumReady()).isTrue();
    assertThat(s.isHybrid()).isTrue();
  }

  @Test
  void postQuantumReadyWhenPrimarySignatureIsPq() {
    CryptoSuite s = new CryptoSuite("a", KemAlgorithm.X25519, null,
        SignatureAlgorithm.ML_DSA_65, null, List.of());
    assertThat(s.isPostQuantumReady()).isTrue();
  }

  @Test
  void postQuantumReadyWhenHybridSignatureIsPq() {
    CryptoSuite s = new CryptoSuite("a", KemAlgorithm.X25519, null,
        SignatureAlgorithm.ED25519, SignatureAlgorithm.ML_DSA_44, List.of());
    assertThat(s.isPostQuantumReady()).isTrue();
    assertThat(s.isHybrid()).isTrue();
  }

  @Test
  void notPostQuantumReadyWhenFullyClassical() {
    CryptoSuite s = new CryptoSuite("a", KemAlgorithm.X25519, null,
        SignatureAlgorithm.ED25519, null, List.of());
    assertThat(s.isPostQuantumReady()).isFalse();
    assertThat(s.isHybrid()).isFalse();
  }

  @Test
  void nullContextsBecomeEmptyList() {
    CryptoSuite s = new CryptoSuite("a", KemAlgorithm.X25519, null,
        SignatureAlgorithm.ED25519, null, null);
    assertThat(s.applicableContexts()).isEmpty();
  }

  @Test
  void requiresName() {
    assertThatThrownBy(() -> new CryptoSuite(null, KemAlgorithm.X25519, null,
        SignatureAlgorithm.ED25519, null, List.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void requiresPrimaryKem() {
    assertThatThrownBy(() -> new CryptoSuite("a", null, null,
        SignatureAlgorithm.ED25519, null, List.of()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void requiresPrimarySignature() {
    assertThatThrownBy(() -> new CryptoSuite("a", KemAlgorithm.X25519, null,
        null, null, List.of()))
        .isInstanceOf(NullPointerException.class);
  }
}
