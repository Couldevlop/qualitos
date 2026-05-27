package com.openlab.qualitos.crypto.application;

import com.openlab.qualitos.crypto.domain.model.CryptoSuite;
import com.openlab.qualitos.crypto.domain.model.KemAlgorithm;
import com.openlab.qualitos.crypto.domain.model.SignatureAlgorithm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Crypto-agility configuration registry (CLAUDE.md §11.4).
 *
 * <p>Defines named suites with their KEM + signature combinations and the
 * contexts where each is applicable. Consumers ask the registry by context
 * (e.g. "tls", "audit-report", "blockchain-block") and get the strongest
 * suite enabled for that context.
 *
 * <p>The default suites SHIPPED are the P3 baseline:
 * <ul>
 *   <li>{@code tls-hybrid-p3}: X25519 + ML-KEM-768 / Ed25519 + ML-DSA-65 — used
 *   for TLS terminators and northbound APIs.</li>
 *   <li>{@code audit-report}: Ed25519 + ML-DSA-65 — hybrid signature for
 *   long-lived signed audit artefacts (PDF reports, certification dossiers).</li>
 *   <li>{@code blockchain-block}: Ed25519 + ML-DSA-65 — hybrid signature for
 *   anchor receipts / blockchain blocks (ADR 0012).</li>
 *   <li>{@code legacy-classical}: X25519 / Ed25519 — fallback for clients
 *   that don't yet support PQ. Tracked in CBOM as DEPRECATED.</li>
 * </ul>
 */
public final class CryptoSuiteConfig {

  private final Map<String, CryptoSuite> suites = new LinkedHashMap<>();

  public CryptoSuiteConfig() {
    registerDefaults();
  }

  private void registerDefaults() {
    register(new CryptoSuite(
        "tls-hybrid-p3",
        KemAlgorithm.X25519,
        KemAlgorithm.ML_KEM_768,
        SignatureAlgorithm.ED25519,
        SignatureAlgorithm.ML_DSA_65,
        List.of("tls", "northbound-api")));

    // §11.4 : signatures hybrides (Ed25519 + ML-DSA-65) pour rapports critiques
    // et blocs blockchain — la garantie survit à la rupture de la courbe classique.
    register(new CryptoSuite(
        "audit-report",
        KemAlgorithm.ML_KEM_768,
        null,
        SignatureAlgorithm.ED25519,
        SignatureAlgorithm.ML_DSA_65,
        List.of("audit-report", "certificate", "dashboard")));

    register(new CryptoSuite(
        "blockchain-block",
        KemAlgorithm.ML_KEM_768,
        null,
        SignatureAlgorithm.ED25519,
        SignatureAlgorithm.ML_DSA_65,
        List.of("blockchain-block")));

    register(new CryptoSuite(
        "legacy-classical",
        KemAlgorithm.X25519,
        null,
        SignatureAlgorithm.ED25519,
        null,
        List.of("legacy-tls")));
  }

  /** Register or replace a suite. */
  public CryptoSuiteConfig register(CryptoSuite suite) {
    if (suite == null || suite.name() == null) {
      throw new IllegalArgumentException("suite + name required");
    }
    suites.put(suite.name(), suite);
    return this;
  }

  public Optional<CryptoSuite> find(String name) {
    return Optional.ofNullable(suites.get(name));
  }

  public List<CryptoSuite> listAll() {
    return List.copyOf(suites.values());
  }

  /**
   * Resolve the preferred suite for a given context.
   * Returns the FIRST suite (by registration order) whose
   * {@link CryptoSuite#applicableContexts()} contains the context AND
   * {@link CryptoSuite#isPostQuantumReady()} == true. Falls back to any matching
   * suite if no PQ-ready one exists.
   */
  public Optional<CryptoSuite> resolveFor(String context) {
    if (context == null) return Optional.empty();
    CryptoSuite fallback = null;
    for (CryptoSuite s : suites.values()) {
      if (s.applicableContexts().contains(context)) {
        if (s.isPostQuantumReady()) return Optional.of(s);
        if (fallback == null) fallback = s;
      }
    }
    return Optional.ofNullable(fallback);
  }
}
