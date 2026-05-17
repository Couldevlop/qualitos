package com.openlab.qualitos.iot.domain.port;

import com.openlab.qualitos.iot.domain.model.TelemetryPoint;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Output port — telemetry persistence to TimescaleDB hypertable. */
public interface TelemetryRepository {
  TelemetryPoint save(TelemetryPoint point);
  List<TelemetryPoint> saveAll(List<TelemetryPoint> points);
  List<TelemetryPoint> findByDevice(UUID tenantId, UUID deviceId, Instant from, Instant to, int limit);
  long countByTenant(UUID tenantId);
}
