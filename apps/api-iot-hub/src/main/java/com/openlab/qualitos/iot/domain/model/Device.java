package com.openlab.qualitos.iot.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * IoT Device (ISA-95 hierarchical asset).
 *
 * <p>Pure domain record — no Spring / no JPA. Persistence lives in
 * {@code infrastructure/persistence/DeviceEntity}.
 *
 * <p>CLAUDE.md §9.6 — Device Registry & Digital Twin. Includes ISA-95
 * positioning (enterprise / site / area / workCenter / equipment) and the
 * X.509 cert fingerprint for mTLS device authentication.
 */
public record Device(
    UUID id,
    UUID tenantId,
    String code,
    String name,
    DeviceType type,
    Protocol protocol,
    String enterprise,
    String site,
    String area,
    String workCenter,
    String equipment,
    String certFingerprintSha256,
    Map<String, Object> twin,
    Instant provisionedAt,
    Instant lastSeenAt
) {

  public Device {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(protocol, "protocol");
    Objects.requireNonNull(provisionedAt, "provisionedAt");
    twin = twin == null ? Map.of() : Map.copyOf(twin);
  }
}
