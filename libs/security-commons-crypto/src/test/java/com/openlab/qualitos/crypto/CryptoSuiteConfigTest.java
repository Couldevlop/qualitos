package com.openlab.qualitos.crypto;

import com.openlab.qualitos.crypto.application.CryptoSuiteConfig;
import com.openlab.qualitos.crypto.domain.model.CryptoSuite;
import com.openlab.qualitos.crypto.domain.model.KemAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoSuiteConfigTest {

  @Test
  void defaultSuitesRegistered() {
    CryptoSuiteConfig cfg = new CryptoSuiteConfig();
    assertThat(cfg.listAll()).hasSize(4);
    assertThat(cfg.find("tls-hybrid-p3")).isPresent();
    assertThat(cfg.find("audit-report")).isPresent();
    assertThat(cfg.find("blockchain-block")).isPresent();
    assertThat(cfg.find("legacy-classical")).isPresent();
  }

  @Test
  void hybridSuiteIsPostQuantumReady() {
    CryptoSuite s = new CryptoSuiteConfig().find("tls-hybrid-p3").orElseThrow();
    assertThat(s.isHybrid()).isTrue();
    assertThat(s.isPostQuantumReady()).isTrue();
    assertThat(s.primaryKem()).isEqualTo(KemAlgorithm.X25519);
    assertThat(s.hybridKem()).isEqualTo(KemAlgorithm.ML_KEM_768);
    assertThat(s.primarySignature()).isEqualTo(SignatureAlgorithm.ED25519);
    assertThat(s.hybridSignature()).isEqualTo(SignatureAlgorithm.ML_DSA_65);
  }

  @Test
  void auditAndBlockchainSuitesAreHybridSignatures() {
    CryptoSuiteConfig cfg = new CryptoSuiteConfig();
    for (String name : List.of("audit-report", "blockchain-block")) {
      CryptoSuite s = cfg.find(name).orElseThrow();
      assertThat(s.primarySignature()).isEqualTo(SignatureAlgorithm.ED25519);
      assertThat(s.hybridSignature()).isEqualTo(SignatureAlgorithm.ML_DSA_65);
      assertThat(s.isPostQuantumReady()).isTrue();
    }
  }

  @Test
  void resolveForReturnsPqSuiteFirst() {
    CryptoSuite s = new CryptoSuiteConfig().resolveFor("blockchain-block").orElseThrow();
    assertThat(s.isPostQuantumReady()).isTrue();
    assertThat(s.name()).isEqualTo("blockchain-block");
  }

  @Test
  void resolveForFallsBackToNonPqSuiteWhenNoPqMatch() {
    // "legacy-tls" only matches legacy-classical, which is NOT PQ-ready → fallback path.
    CryptoSuite s = new CryptoSuiteConfig().resolveFor("legacy-tls").orElseThrow();
    assertThat(s.name()).isEqualTo("legacy-classical");
    assertThat(s.isPostQuantumReady()).isFalse();
  }

  @Test
  void resolveForUnknownContextEmpty() {
    assertThat(new CryptoSuiteConfig().resolveFor("non-existent")).isEmpty();
  }

  @Test
  void resolveForNullContextEmpty() {
    assertThat(new CryptoSuiteConfig().resolveFor(null)).isEmpty();
  }

  @Test
  void canRegisterCustomSuite() {
    CryptoSuiteConfig cfg = new CryptoSuiteConfig().register(new CryptoSuite(
        "mlkem-512-only",
        KemAlgorithm.ML_KEM_512, null,
        SignatureAlgorithm.ML_DSA_44, null,
        List.of("internal")));
    assertThat(cfg.find("mlkem-512-only")).isPresent();
  }

  @Test
  void registerRejectsNull() {
    CryptoSuiteConfig cfg = new CryptoSuiteConfig();
    assertThatThrownBy(() -> cfg.register(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void legacyClassicalIsNotPostQuantumReady() {
    CryptoSuite s = new CryptoSuiteConfig().find("legacy-classical").orElseThrow();
    assertThat(s.isPostQuantumReady()).isFalse();
    assertThat(s.isHybrid()).isFalse();
  }

  @Test
  void allAlgorithmsExposeOidAndSize() {
    for (KemAlgorithm k : KemAlgorithm.values()) {
      assertThat(k.oid()).isNotBlank();
      assertThat(k.sharedSecretBytes()).isPositive();
    }
    for (SignatureAlgorithm s : SignatureAlgorithm.values()) {
      assertThat(s.oid()).isNotBlank();
      assertThat(s.signatureBytes()).isPositive();
    }
  }
}
