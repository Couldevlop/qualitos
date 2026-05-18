package com.openlab.qualitos.iot.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A single telemetry sample for a device.
 *
 * <p>Stored in a TimescaleDB hypertable {@code iot_telemetry} for efficient
 * time-series queries (CLAUDE.md §9.3 + §10.2).
 */
public record TelemetryPoint(
    UUID id,
    UUID tenantId,
    UUID deviceId,
    String metric,
    Double value,
    String unit,
    Instant recordedAt
) {
  public TelemetryPoint {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(deviceId, "deviceId");
    Objects.requireNonNull(metric, "metric");
    Objects.requireNonNull(recordedAt, "recordedAt");
    if (metric.length() > 100) throw new IllegalArgumentException("metric too long");
  }
}
