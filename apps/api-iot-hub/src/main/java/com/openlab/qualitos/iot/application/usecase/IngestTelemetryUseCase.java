package com.openlab.qualitos.iot.application.usecase;

import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.model.ThresholdBreachEvent;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import com.openlab.qualitos.iot.domain.port.NonConformancePublisher;
import com.openlab.qualitos.iot.domain.port.TelemetryRepository;
import com.openlab.qualitos.iot.domain.service.DeviceShadow;
import com.openlab.qualitos.iot.domain.service.StreamRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Use case — ingest a single or batch of telemetry points and run the rule engine.
 *
 * <p>OWASP A03: device existence is verified per-tenant (no IDOR — A01).
 */
public final class IngestTelemetryUseCase {

  private static final Logger LOG = LoggerFactory.getLogger(IngestTelemetryUseCase.class);

  private final DeviceRepository deviceRepo;
  private final TelemetryRepository telemetryRepo;
  private final StreamRuleEngine ruleEngine;
  private final NonConformancePublisher publisher;

  public IngestTelemetryUseCase(
      DeviceRepository deviceRepo,
      TelemetryRepository telemetryRepo,
      StreamRuleEngine ruleEngine,
      NonConformancePublisher publisher) {
    this.deviceRepo = Objects.requireNonNull(deviceRepo);
    this.telemetryRepo = Objects.requireNonNull(telemetryRepo);
    this.ruleEngine = Objects.requireNonNull(ruleEngine);
    this.publisher = Objects.requireNonNull(publisher);
  }

  public TelemetryPoint ingest(
      UUID tenantId, UUID deviceId, String metric, Double value, String unit, Instant recordedAt) {
    Objects.requireNonNull(tenantId);
    Objects.requireNonNull(deviceId);
    Device device = deviceRepo.findById(tenantId, deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));

    TelemetryPoint point = new TelemetryPoint(
        UUID.randomUUID(), tenantId, device.id(), metric, value, unit,
        recordedAt == null ? Instant.now() : recordedAt);
    // Persistance de la mesure = ingestion proprement dite (aucun @Transactional
    // englobant : ce save commit immédiatement dans sa propre transaction).
    TelemetryPoint saved = telemetryRepo.save(point);

    // Effets secondaires « best-effort » : last-seen, Device Shadow (§9.6) et règles
    // d'alerte (qui notifient le quality-engine via un appel egress). Un hoquet ici
    // — engine indisponible, listener en échec, contention sur la ligne device sous
    // charge CI — ne doit JAMAIS faire perdre/sous-compter une mesure DÉJÀ persistée
    // (sinon une métrique valide remonte à tort comme « dropped »). La mesure est
    // déjà committée : on journalise et on poursuit.
    try {
      deviceRepo.touchLastSeen(tenantId, device.id(), saved.recordedAt());
      deviceRepo.updateTwin(tenantId, device.id(), DeviceShadow.mergeReported(
          device.twin(), saved.metric(), saved.value(), saved.unit(), saved.recordedAt()));
      for (ThresholdBreachEvent breach : ruleEngine.evaluate(saved)) {
        publisher.notifyBreach(breach);
      }
    } catch (RuntimeException ex) {
      LOG.warn("Telemetry side-effects failed after persistence (device={} metric={}): {}",
          device.id(), metric, ex.getMessage());
    }
    return saved;
  }

  public List<TelemetryPoint> ingestBatch(
      UUID tenantId, UUID deviceId, List<TelemetryPoint> points) {
    Objects.requireNonNull(tenantId);
    Objects.requireNonNull(deviceId);
    if (points == null || points.isEmpty()) return List.of();
    if (points.size() > 1000) {
      throw new IllegalArgumentException("batch too large (max 1000)");
    }
    Device device = deviceRepo.findById(tenantId, deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));

    List<TelemetryPoint> toSave = new ArrayList<>(points.size());
    for (TelemetryPoint p : points) {
      toSave.add(new TelemetryPoint(
          UUID.randomUUID(), tenantId, device.id(),
          p.metric(), p.value(), p.unit(),
          p.recordedAt() == null ? Instant.now() : p.recordedAt()));
    }
    List<TelemetryPoint> saved = telemetryRepo.saveAll(toSave);
    if (!saved.isEmpty()) {
      deviceRepo.touchLastSeen(tenantId, device.id(),
          saved.get(saved.size() - 1).recordedAt());
      // Device Shadow : fusionne toutes les mesures du lot (dernière valeur par métrique),
      // puis une seule écriture du twin.
      Map<String, Object> twin = device.twin();
      for (TelemetryPoint p : saved) {
        twin = DeviceShadow.mergeReported(twin, p.metric(), p.value(), p.unit(), p.recordedAt());
      }
      deviceRepo.updateTwin(tenantId, device.id(), twin);
    }
    for (TelemetryPoint p : saved) {
      for (ThresholdBreachEvent breach : ruleEngine.evaluate(p)) {
        publisher.notifyBreach(breach);
      }
    }
    return saved;
  }

  public static class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(UUID id) {
      super("Device not found: " + id);
    }
  }
}
