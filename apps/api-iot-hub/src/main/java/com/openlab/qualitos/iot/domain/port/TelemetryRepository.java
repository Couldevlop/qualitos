package com.openlab.qualitos.iot.domain.port;

import com.openlab.qualitos.iot.domain.model.RollupBucket;
import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.model.TelemetryRollup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Output port — telemetry persistence to TimescaleDB hypertable. */
public interface TelemetryRepository {
  TelemetryPoint save(TelemetryPoint point);
  List<TelemetryPoint> saveAll(List<TelemetryPoint> points);
  List<TelemetryPoint> findByDevice(UUID tenantId, UUID deviceId, Instant from, Instant to, int limit);
  long countByTenant(UUID tenantId);

  /**
   * Agrège la télémétrie d'un équipement par tranche temporelle (avg/min/max/count),
   * tenant-scopé (OWASP A01 — pas d'IDOR). Calcul SQL standard ({@code date_trunc} +
   * agrégats) portable PostgreSQL/H2 ; sur runtime TimescaleDB la continuous aggregate
   * {@code iot_telemetry_hourly} sert d'accélérateur, mais le résultat est identique.
   *
   * @param tenantId tenant issu du JWT — jamais du payload
   * @param metric   métrique exacte à agréger (filtre obligatoire)
   * @param bucket   granularité (allow-list)
   * @param limit    nombre maximal de buckets renvoyés (les plus récents d'abord)
   * @return buckets triés du plus récent au plus ancien
   */
  List<TelemetryRollup> rollupByDevice(
      UUID tenantId, UUID deviceId, String metric, RollupBucket bucket, int limit);
}
