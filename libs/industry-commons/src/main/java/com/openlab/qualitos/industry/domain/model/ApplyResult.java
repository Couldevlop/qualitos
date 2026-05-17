package com.openlab.qualitos.industry.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Outcome of applying a pack to a tenant (audit trail entry). */
public record ApplyResult(
    UUID tenantId,
    String packId,
    String packVersion,
    String packSha256,
    Instant appliedAt,
    List<String> activatedConnectors,
    List<String> activatedKpis,
    List<String> activatedNorms
) {
  public ApplyResult {
    Objects.requireNonNull(tenantId);
    Objects.requireNonNull(packId);
    Objects.requireNonNull(appliedAt);
    activatedConnectors = activatedConnectors == null ? List.of() : List.copyOf(activatedConnectors);
    activatedKpis = activatedKpis == null ? List.of() : List.copyOf(activatedKpis);
    activatedNorms = activatedNorms == null ? List.of() : List.copyOf(activatedNorms);
  }
}
