package com.openlab.qualitos.crypto.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Immutable description of an enabled crypto suite.
 *
 * <p>A suite couples a primary KEM/signature with optional hybrid companions.
 * Hybrid mode = primary + companion: the secret is HKDF-combined; signatures are
 * concatenated. This is the recommended PQ-migration strategy (IETF draft-ietf-tls-hybrid-design).
 */
public record CryptoSuite(
    String name,
    KemAlgorithm primaryKem,
    KemAlgorithm hybridKem,                // null = no hybrid KEM
    SignatureAlgorithm primarySignature,
    SignatureAlgorithm hybridSignature,    // null = no hybrid signature
    List<String> applicableContexts        // e.g. "tls", "audit-report", "blockchain-block"
) {

  public CryptoSuite {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(primaryKem, "primaryKem");
    Objects.requireNonNull(primarySignature, "primarySignature");
    applicableContexts = applicableContexts == null ? List.of() : List.copyOf(applicableContexts);
  }

  public boolean isHybrid() {
    return hybridKem != null || hybridSignature != null;
  }

  public boolean isPostQuantumReady() {
    return primaryKem.postQuantum() || (hybridKem != null && hybridKem.postQuantum())
        || primarySignature.postQuantum() || (hybridSignature != null && hybridSignature.postQuantum());
  }
}
