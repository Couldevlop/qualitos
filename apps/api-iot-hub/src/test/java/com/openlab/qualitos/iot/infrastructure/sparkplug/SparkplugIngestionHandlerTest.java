package com.openlab.qualitos.iot.infrastructure.sparkplug;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.model.DeviceType;
import com.openlab.qualitos.iot.domain.model.Protocol;
import com.openlab.qualitos.iot.domain.model.RollupBucket;
import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.model.TelemetryRollup;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import com.openlab.qualitos.iot.domain.port.NonConformancePublisher;
import com.openlab.qualitos.iot.domain.port.TelemetryRepository;
import com.openlab.qualitos.iot.domain.service.StreamRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Handler d'ingestion Sparkplug B : résolution tenant via {@code findUniqueByCode}
 * (fail-closed cross-tenant), robustesse, tenant effectivement persisté — unitaire pur.
 *
 * <p>{@link IngestTelemetryUseCase} étant {@code final}, on pilote un use case RÉEL
 * adossé à des stubs in-memory (mêmes conventions que {@code FhirIngestionHandlerTest}).
 */
class SparkplugIngestionHandlerTest {

  private static final UUID TENANT = UUID.randomUUID();
  private static final UUID OTHER_TENANT = UUID.randomUUID();

  private final ObjectMapper json = new ObjectMapper();

  private InMemoryDeviceRepo deviceRepo;
  private RecordingTelemetryRepo telemetryRepo;
  private SparkplugIngestionHandler handler;

  @BeforeEach
  void setUp() {
    deviceRepo = new InMemoryDeviceRepo();
    telemetryRepo = new RecordingTelemetryRepo();
    StreamRuleEngine engine = new StreamRuleEngine((t, d, m) -> List.of());
    NonConformancePublisher publisher = e -> { };
    IngestTelemetryUseCase ingestUseCase =
        new IngestTelemetryUseCase(deviceRepo, telemetryRepo, engine, publisher);
    handler = new SparkplugIngestionHandler(
        deviceRepo, ingestUseCase, new SparkplugBDecoder(), 500);
  }

  private JsonNode parse(String s) throws Exception {
    return json.readTree(s);
  }

  private Device register(UUID tenantId, String code) {
    Device d = new Device(UUID.randomUUID(), tenantId, code, "Cobot",
        DeviceType.SENSOR_VIBRATION, Protocol.SPARKPLUG_B,
        null, null, null, null, null, null, Map.of(), Instant.now(), null);
    deviceRepo.save(d);
    return d;
  }

  private static String ddata(String deviceCode) {
    return """
        {"groupId":"g","edgeNodeId":"node-1","deviceId":"%s",
         "metrics":[
           {"name":"Temperature","value":4.2,"unit":"degC"},
           {"name":"Vibration","value":1.5,"unit":"g"}
         ],
         "timestamp":1749722400000}""".formatted(deviceCode);
  }

  @Test
  @DisplayName("DDATA + device connu → ingestion avec le tenant DU REGISTRE (pas du payload)")
  void validPayloadIsIngested() throws Exception {
    Device d = register(TENANT, "capteur-01");

    var outcome = handler.handle(parse(ddata("capteur-01")));

    assertThat(outcome.ingested()).isEqualTo(2);
    assertThat(outcome.dropped()).isZero();
    assertThat(outcome.issues()).isEmpty();
    assertThat(telemetryRepo.saved).hasSize(2);
    TelemetryPoint p = telemetryRepo.saved.get(0);
    assertThat(p.tenantId()).isEqualTo(TENANT);
    assertThat(p.deviceId()).isEqualTo(d.id());
    assertThat(p.metric()).isEqualTo("Temperature");
    assertThat(p.value()).isEqualTo(4.2);
    assertThat(p.recordedAt()).isEqualTo(Instant.ofEpochMilli(1749722400000L));
  }

  @Test
  @DisplayName("Device inconnu → rejet fail-closed, aucune ingestion")
  void unknownDeviceIsDropped() throws Exception {
    var outcome = handler.handle(parse(ddata("inconnu")));

    assertThat(outcome.ingested()).isZero();
    assertThat(outcome.dropped()).isEqualTo(1);
    assertThat(outcome.issues()).singleElement().asString().contains("unknown/ambiguous device");
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  @DisplayName("Code device en collision cross-tenant → fail-closed (anti-IDOR A01)")
  void crossTenantCollisionIsFailClosed() throws Exception {
    // Même code chez deux tenants → findUniqueByCode renvoie empty → rejet.
    register(TENANT, "shared-code");
    register(OTHER_TENANT, "shared-code");

    var outcome = handler.handle(parse(ddata("shared-code")));

    assertThat(outcome.ingested()).isZero();
    assertThat(outcome.dropped()).isEqualTo(1);
    assertThat(outcome.issues()).singleElement().asString().contains("ambiguous");
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  @DisplayName("Payload pourri (pas d'objet / pas de métrique) → rejet motivé, ne lève pas")
  void rottenPayloadIsDroppedGracefully() throws Exception {
    assertThat(handler.handle(null).dropped()).isEqualTo(1);
    assertThat(handler.handle(parse("{\"deviceId\":\"x\"}")).dropped()).isEqualTo(1);
    assertThat(handler.handle(parse("[]")).dropped()).isEqualTo(1);
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  @DisplayName("Au-delà de la borne maxMetrics → troncature signalée (OWASP A04)")
  void payloadIsTruncatedAtMaxMetrics() throws Exception {
    register(TENANT, "capteur-01");
    handler = new SparkplugIngestionHandler(
        deviceRepo, new IngestTelemetryUseCase(deviceRepo, telemetryRepo,
            new StreamRuleEngine((t, d, m) -> List.of()), e -> { }),
        new SparkplugBDecoder(), 1);

    var outcome = handler.handle(parse(ddata("capteur-01")));

    assertThat(outcome.ingested()).isEqualTo(1);
    assertThat(outcome.issues()).anySatisfy(i -> assertThat(i).contains("truncated"));
  }

  @Test
  @DisplayName("Une métrique en échec aval n'interrompt pas le reste du lot")
  void failingMetricDoesNotAbortBatch() throws Exception {
    register(TENANT, "capteur-01");
    telemetryRepo.throwOnSaveOnce = true; // la 1re save lève, la 2e passe

    var outcome = handler.handle(parse(ddata("capteur-01")));

    assertThat(outcome.ingested()).isEqualTo(1);
    assertThat(outcome.dropped()).isEqualTo(1);
    assertThat(outcome.issues()).anySatisfy(i -> assertThat(i).contains("ingestion failed"));
    assertThat(telemetryRepo.saved).hasSize(1);
  }

  // ---- stubs in-memory (mêmes conventions que FhirIngestionHandlerTest) -------

  static class InMemoryDeviceRepo implements DeviceRepository {
    final Map<UUID, Device> byId = new LinkedHashMap<>();

    @Override public Device save(Device d) { byId.put(d.id(), d); return d; }
    @Override public Optional<Device> findById(UUID t, UUID i) {
      Device d = byId.get(i);
      return d != null && d.tenantId().equals(t) ? Optional.of(d) : Optional.empty();
    }
    @Override public Optional<Device> findByCode(UUID t, String c) {
      return byId.values().stream()
          .filter(d -> d.tenantId().equals(t) && d.code().equals(c)).findFirst();
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
    @Override public void updateTwin(UUID t, UUID id, Map<String, Object> twin) {
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
    boolean throwOnSaveOnce = false;

    @Override public TelemetryPoint save(TelemetryPoint p) {
      if (throwOnSaveOnce) {
        throwOnSaveOnce = false;
        throw new RuntimeException("downstream boom");
      }
      saved.add(p);
      return p;
    }
    @Override public List<TelemetryPoint> saveAll(List<TelemetryPoint> pts) {
      saved.addAll(pts);
      return pts;
    }
    @Override public List<TelemetryPoint> findByDevice(UUID t, UUID d, Instant from, Instant to, int lim) {
      return saved;
    }
    @Override public long countByTenant(UUID t) { return saved.size(); }
    @Override public List<TelemetryRollup> rollupByDevice(
        UUID t, UUID d, String metric, RollupBucket bucket, int limit) {
      return List.of();
    }
  }
}
