package com.openlab.qualitos.iot.application.usecase;

import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.model.RollupBucket;
import com.openlab.qualitos.iot.domain.model.TelemetryRollup;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import com.openlab.qualitos.iot.domain.port.TelemetryRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Use case — agrège la télémétrie d'un équipement en buckets temporels (CLAUDE.md §9.3,
 * §6.4) : moyenne / min / max / nombre par tranche.
 *
 * <p>OWASP A01 : l'existence de l'équipement est vérifiée <b>pour le tenant</b> (résolu
 * du JWT en amont) avant toute agrégation — un device d'un autre tenant est invisible
 * (fail-closed, pas d'IDOR). La métrique est obligatoire (évite un balayage non borné).
 */
public final class RollupTelemetryUseCase {

  /** Borne dure du nombre de buckets renvoyés (anti-DoS, OWASP A04). */
  public static final int MAX_LIMIT = 1000;
  private static final int DEFAULT_LIMIT = 168; // 7 jours en buckets horaires

  private final DeviceRepository deviceRepo;
  private final TelemetryRepository telemetryRepo;

  public RollupTelemetryUseCase(DeviceRepository deviceRepo, TelemetryRepository telemetryRepo) {
    this.deviceRepo = Objects.requireNonNull(deviceRepo, "deviceRepo");
    this.telemetryRepo = Objects.requireNonNull(telemetryRepo, "telemetryRepo");
  }

  public List<TelemetryRollup> rollup(
      UUID tenantId, UUID deviceId, String metric, RollupBucket bucket, Integer limit) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(deviceId, "deviceId");
    if (metric == null || metric.isBlank()) {
      throw new IllegalArgumentException("metric is required");
    }
    RollupBucket effectiveBucket = bucket == null ? RollupBucket.HOUR : bucket;
    int effectiveLimit = clampLimit(limit);

    // Tenant-scopé : device d'un autre tenant introuvable → 404 (anti-IDOR A01).
    Device device = deviceRepo.findById(tenantId, deviceId)
        .orElseThrow(() -> new IngestTelemetryUseCase.DeviceNotFoundException(deviceId));

    return telemetryRepo.rollupByDevice(
        tenantId, device.id(), metric, effectiveBucket, effectiveLimit);
  }

  private static int clampLimit(Integer limit) {
    if (limit == null || limit <= 0) {
      return DEFAULT_LIMIT;
    }
    return Math.min(limit, MAX_LIMIT);
  }
}
