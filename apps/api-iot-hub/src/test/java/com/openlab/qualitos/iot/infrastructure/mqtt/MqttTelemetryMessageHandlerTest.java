package com.openlab.qualitos.iot.infrastructure.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.model.DeviceType;
import com.openlab.qualitos.iot.domain.model.Protocol;
import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.model.ThresholdBreachEvent;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import com.openlab.qualitos.iot.domain.port.NonConformancePublisher;
import com.openlab.qualitos.iot.domain.port.TelemetryRepository;
import com.openlab.qualitos.iot.domain.service.StreamRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Isolated unit test of the pure MQTT message handler. No broker, no Spring context.
 *
 * <p>{@link IngestTelemetryUseCase} is {@code final} (cannot be Mockito-mocked), so we drive
 * a real use case backed by in-memory stub collaborators — the same pattern used by
 * {@code IngestTelemetryUseCaseTest}. This still verifies end-to-end mapping AND the tenant
 * actually persisted on the telemetry point.
 */
class MqttTelemetryMessageHandlerTest {

  private InMemoryDeviceRepo deviceRepo;
  private RecordingTelemetryRepo telemetryRepo;
  private IngestTelemetryUseCase ingestUseCase;
  private MqttTelemetryMessageHandler handler;

  private static final UUID DEVICE_TENANT = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    deviceRepo = new InMemoryDeviceRepo();
    telemetryRepo = new RecordingTelemetryRepo();
    StreamRuleEngine engine = new StreamRuleEngine((t, d, m) -> List.of());
    NonConformancePublisher publisher = e -> { };
    ingestUseCase = new IngestTelemetryUseCase(deviceRepo, telemetryRepo, engine, publisher);
    handler = new MqttTelemetryMessageHandler(deviceRepo, ingestUseCase, new ObjectMapper());
  }

  private Device register(UUID tenantId, String code) {
    Device d = new Device(UUID.randomUUID(), tenantId, code, "Fridge",
        DeviceType.SENSOR_TEMPERATURE, Protocol.MQTT,
        null, null, null, null, null, null, Map.of(), Instant.now(), null);
    deviceRepo.save(d);
    return d;
  }

  private static byte[] bytes(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void mapsTopicAndPayloadToIngestionWithTenantFromDevice() {
    Device d = register(DEVICE_TENANT, "FRIDGE-01");

    boolean ingested = handler.handle(
        "qualitos/FRIDGE-01/temperature",
        bytes("{\"value\": 4.2, \"unit\": \"C\", \"recordedAt\": \"2026-05-29T10:15:30Z\"}"));

    assertThat(ingested).isTrue();
    assertThat(telemetryRepo.saved).hasSize(1);
    TelemetryPoint p = telemetryRepo.saved.get(0);
    assertThat(p.tenantId()).isEqualTo(DEVICE_TENANT);
    assertThat(p.deviceId()).isEqualTo(d.id());
    assertThat(p.metric()).isEqualTo("temperature");
    assertThat(p.value()).isEqualTo(4.2);
    assertThat(p.unit()).isEqualTo("C");
    assertThat(p.recordedAt()).isEqualTo(Instant.parse("2026-05-29T10:15:30Z"));
  }

  @Test
  void tenantIsTakenFromDeviceNotFromPayload() {
    // Payload tries to inject a foreign tenantId — it MUST be ignored.
    UUID attackerTenant = UUID.randomUUID();
    register(DEVICE_TENANT, "FRIDGE-01");

    handler.handle(
        "qualitos/FRIDGE-01/temperature",
        bytes("{\"tenantId\": \"" + attackerTenant + "\", \"value\": 9.9}"));

    assertThat(telemetryRepo.saved).hasSize(1);
    assertThat(telemetryRepo.saved.get(0).tenantId())
        .isEqualTo(DEVICE_TENANT)
        .isNotEqualTo(attackerTenant);
  }

  @Test
  void deviceCodeFromPayloadOverridesTopic() {
    Device d = register(DEVICE_TENANT, "PAYLOAD-DEV");

    boolean ingested = handler.handle(
        "qualitos/IGNORED-TOPIC-CODE/temp",
        bytes("{\"deviceCode\": \"PAYLOAD-DEV\", \"metric\": \"pressure\", \"value\": 1.0}"));

    assertThat(ingested).isTrue();
    assertThat(telemetryRepo.saved).hasSize(1);
    assertThat(telemetryRepo.saved.get(0).deviceId()).isEqualTo(d.id());
    assertThat(telemetryRepo.saved.get(0).metric()).isEqualTo("pressure");
  }

  @Test
  void unknownDeviceIsIgnoredNoIngestion() {
    boolean ingested = handler.handle(
        "qualitos/GHOST/temp", bytes("{\"value\": 1.0}"));

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void ambiguousCodeFailsClosed() {
    // Same code provisioned under two tenants -> findUniqueByCode returns empty.
    register(DEVICE_TENANT, "DUP");
    register(UUID.randomUUID(), "DUP");

    boolean ingested = handler.handle("qualitos/DUP/temp", bytes("{\"value\": 2.0}"));

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void malformedJsonIsIgnored() {
    boolean ingested = handler.handle("qualitos/FRIDGE-01/temp", bytes("{not-json"));

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void nonObjectPayloadIsIgnored() {
    boolean ingested = handler.handle("qualitos/FRIDGE-01/temp", bytes("[1,2,3]"));

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void missingValueIsIgnored() {
    register(DEVICE_TENANT, "FRIDGE-01");
    boolean ingested = handler.handle(
        "qualitos/FRIDGE-01/temp", bytes("{\"unit\": \"C\"}"));

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void nonNumericValueIsIgnored() {
    register(DEVICE_TENANT, "FRIDGE-01");
    boolean ingested = handler.handle(
        "qualitos/FRIDGE-01/temp", bytes("{\"value\": \"hot\"}"));

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void missingMetricIsIgnored() {
    register(DEVICE_TENANT, "FRIDGE-01");
    // Topic has no metric segment and payload provides none.
    boolean ingested = handler.handle(
        "qualitos/FRIDGE-01", bytes("{\"value\": 1.0}"));

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void missingDeviceCodeIsIgnored() {
    boolean ingested = handler.handle(
        "qualitos//temp", bytes("{\"value\": 1.0}"));

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void invalidRecordedAtFallsBackToServerTime() {
    register(DEVICE_TENANT, "FRIDGE-01");
    Instant before = Instant.now();

    boolean ingested = handler.handle(
        "qualitos/FRIDGE-01/temp",
        bytes("{\"value\": 5.0, \"recordedAt\": \"not-a-date\"}"));

    assertThat(ingested).isTrue();
    // Server-side timestamp applied by the use case (>= test start).
    assertThat(telemetryRepo.saved).hasSize(1);
    assertThat(telemetryRepo.saved.get(0).recordedAt()).isAfterOrEqualTo(before);
  }

  @Test
  void downstreamExceptionIsSwallowed() {
    // A failure deeper in the pipeline must never propagate out of handle()
    // (a single bad message must not tear down the subscription).
    register(DEVICE_TENANT, "FRIDGE-01");
    telemetryRepo.throwOnSave = true;

    boolean ingested = handler.handle(
        "qualitos/FRIDGE-01/temp", bytes("{\"value\": 1.0}"));

    assertThat(ingested).isFalse();
  }

  @Test
  void nullPayloadIsIgnored() {
    boolean ingested = handler.handle("qualitos/FRIDGE-01/temp", null);

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  // ---- in-memory stubs -----------------------------------------------------

  static class InMemoryDeviceRepo implements DeviceRepository {
    final Map<UUID, Device> byId = new LinkedHashMap<>();

    @Override public Device save(Device d) { byId.put(d.id(), d); return d; }
    @Override public Optional<Device> findById(UUID t, UUID i) {
      Device d = byId.get(i);
      return d != null && d.tenantId().equals(t) ? Optional.of(d) : Optional.empty();
    }
    @Override public Optional<Device> findByCode(UUID t, String c) {
      return byId.values().stream().filter(d -> d.tenantId().equals(t) && d.code().equals(c)).findFirst();
    }
    @Override public Optional<Device> findUniqueByCode(String c) {
      var matches = byId.values().stream().filter(d -> d.code().equals(c)).toList();
      return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }
    @Override public List<Device> findAllByTenant(UUID t) {
      return byId.values().stream().filter(d -> d.tenantId().equals(t)).toList();
    }
    @Override public void touchLastSeen(UUID t, UUID id, Instant when) { }
    @Override public long countByTenant(UUID t) { return findAllByTenant(t).size(); }
    @Override public void updateTwin(UUID t, UUID id, java.util.Map<String,Object> twin) {
      Device d = byId.get(id);
      if (d != null && d.tenantId().equals(t)) {
        byId.put(id, new Device(d.id(), d.tenantId(), d.code(), d.name(), d.type(), d.protocol(),
            d.enterprise(), d.site(), d.area(), d.workCenter(), d.equipment(),
            d.certFingerprintSha256(), twin, d.provisionedAt(), d.lastSeenAt()));
      }
    }
  }

  static class RecordingTelemetryRepo implements TelemetryRepository {
    final List<TelemetryPoint> saved = new ArrayList<>();
    boolean throwOnSave = false;
    @Override public TelemetryPoint save(TelemetryPoint p) {
      if (throwOnSave) throw new RuntimeException("downstream boom");
      saved.add(p); return p;
    }
    @Override public List<TelemetryPoint> saveAll(List<TelemetryPoint> pts) { saved.addAll(pts); return pts; }
    @Override public List<TelemetryPoint> findByDevice(UUID t, UUID d, Instant from, Instant to, int lim) {
      return saved;
    }
    @Override public long countByTenant(UUID t) { return saved.size(); }
  }
}
