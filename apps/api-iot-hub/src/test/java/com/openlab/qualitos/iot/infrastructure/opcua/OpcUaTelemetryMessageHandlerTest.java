package com.openlab.qualitos.iot.infrastructure.opcua;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.model.DeviceType;
import com.openlab.qualitos.iot.domain.model.Protocol;
import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import com.openlab.qualitos.iot.domain.port.NonConformancePublisher;
import com.openlab.qualitos.iot.domain.port.TelemetryRepository;
import com.openlab.qualitos.iot.domain.service.StreamRuleEngine;
import org.junit.jupiter.api.BeforeEach;
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
 * Isolated unit test of the pure OPC-UA message handler. No OPC-UA server, no Spring context,
 * no Eclipse Milo types involved.
 *
 * <p>{@link IngestTelemetryUseCase} is {@code final} (cannot be Mockito-mocked), so we drive a
 * real use case backed by in-memory stub collaborators — the same pattern used by the MQTT
 * handler test. This verifies the node→device mapping AND that the tenant actually persisted on
 * the telemetry point is the one taken from the device record (never from the wire).
 */
class OpcUaTelemetryMessageHandlerTest {

  private static final UUID DEVICE_TENANT = UUID.randomUUID();

  private static final String NODE_TEMP = "ns=2;s=Fridge01.Temperature";
  private static final String NODE_PRESS = "ns=2;s=Press01.Pressure";

  private InMemoryDeviceRepo deviceRepo;
  private RecordingTelemetryRepo telemetryRepo;
  private IngestTelemetryUseCase ingestUseCase;
  private OpcUaTelemetryMessageHandler handler;

  @BeforeEach
  void setUp() {
    deviceRepo = new InMemoryDeviceRepo();
    telemetryRepo = new RecordingTelemetryRepo();
    StreamRuleEngine engine = new StreamRuleEngine((t, d, m) -> List.of());
    NonConformancePublisher publisher = e -> { };
    ingestUseCase = new IngestTelemetryUseCase(deviceRepo, telemetryRepo, engine, publisher);

    handler = new OpcUaTelemetryMessageHandler(deviceRepo, ingestUseCase, List.of(
        mapping(NODE_TEMP, "FRIDGE-01", "temperature", "C"),
        mapping(NODE_PRESS, "PRESS-01", "pressure", "bar")));
  }

  private static OpcUaProperties.NodeMapping mapping(String nodeId, String code, String metric, String unit) {
    OpcUaProperties.NodeMapping m = new OpcUaProperties.NodeMapping();
    m.setNodeId(nodeId);
    m.setDeviceCode(code);
    m.setMetric(metric);
    m.setUnit(unit);
    return m;
  }

  private Device register(UUID tenantId, String code) {
    Device d = new Device(UUID.randomUUID(), tenantId, code, code,
        DeviceType.SENSOR_TEMPERATURE, Protocol.OPC_UA,
        null, null, null, null, null, null, Map.of(), Instant.now(), null);
    deviceRepo.save(d);
    return d;
  }

  @Test
  void mapsNodeAndValueToIngestionWithTenantFromDevice() {
    Device d = register(DEVICE_TENANT, "FRIDGE-01");
    Instant src = Instant.parse("2026-05-29T10:15:30Z");

    boolean ingested = handler.handle(NODE_TEMP, 4.2, true, src);

    assertThat(ingested).isTrue();
    assertThat(telemetryRepo.saved).hasSize(1);
    TelemetryPoint p = telemetryRepo.saved.get(0);
    assertThat(p.tenantId()).isEqualTo(DEVICE_TENANT);
    assertThat(p.deviceId()).isEqualTo(d.id());
    assertThat(p.metric()).isEqualTo("temperature");
    assertThat(p.value()).isEqualTo(4.2);
    assertThat(p.unit()).isEqualTo("C");
    assertThat(p.recordedAt()).isEqualTo(src);
  }

  @Test
  void tenantIsTakenFromDeviceNotConfigurable() {
    // The mapping carries no tenant; the device record is the single source of truth.
    Device d = register(DEVICE_TENANT, "FRIDGE-01");

    handler.handle(NODE_TEMP, 9.9, true, null);

    assertThat(telemetryRepo.saved).hasSize(1);
    assertThat(telemetryRepo.saved.get(0).tenantId()).isEqualTo(DEVICE_TENANT);
  }

  @Test
  void unmappedNodeIsIgnored() {
    register(DEVICE_TENANT, "FRIDGE-01");

    boolean ingested = handler.handle("ns=2;s=Unknown.Node", 1.0, true, null);

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void unknownDeviceIsIgnoredNoIngestion() {
    // Node is mapped but the device code does not exist in the registry.
    boolean ingested = handler.handle(NODE_TEMP, 1.0, true, null);

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void ambiguousCodeFailsClosed() {
    // Same code provisioned under two tenants -> findUniqueByCode returns empty.
    register(DEVICE_TENANT, "FRIDGE-01");
    register(UUID.randomUUID(), "FRIDGE-01");

    boolean ingested = handler.handle(NODE_TEMP, 2.0, true, null);

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void badStatusIsIgnored() {
    register(DEVICE_TENANT, "FRIDGE-01");

    boolean ingested = handler.handle(NODE_TEMP, 4.2, false, null);

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void nullValueIsIgnored() {
    register(DEVICE_TENANT, "FRIDGE-01");

    boolean ingested = handler.handle(NODE_TEMP, null, true, null);

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void nanValueIsIgnored() {
    register(DEVICE_TENANT, "FRIDGE-01");

    boolean ingested = handler.handle(NODE_TEMP, Double.NaN, true, null);

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void infiniteValueIsIgnored() {
    register(DEVICE_TENANT, "FRIDGE-01");

    boolean ingested = handler.handle(NODE_TEMP, Double.POSITIVE_INFINITY, true, null);

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void blankNodeIdIsIgnored() {
    boolean ingested = handler.handle("  ", 1.0, true, null);

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void nullNodeIdIsIgnored() {
    boolean ingested = handler.handle(null, 1.0, true, null);

    assertThat(ingested).isFalse();
    assertThat(telemetryRepo.saved).isEmpty();
  }

  @Test
  void nullSourceTimeFallsBackToServerTime() {
    register(DEVICE_TENANT, "FRIDGE-01");
    Instant before = Instant.now();

    boolean ingested = handler.handle(NODE_TEMP, 5.0, true, null);

    assertThat(ingested).isTrue();
    assertThat(telemetryRepo.saved).hasSize(1);
    assertThat(telemetryRepo.saved.get(0).recordedAt()).isAfterOrEqualTo(before);
  }

  @Test
  void booleanValueIsNotRejectedByHandler() {
    // The connector converts booleans to 1.0/0.0 before the handler; the handler accepts numerics.
    register(DEVICE_TENANT, "PRESS-01");

    boolean ingested = handler.handle(NODE_PRESS, 1.0, true, null);

    assertThat(ingested).isTrue();
    assertThat(telemetryRepo.saved).hasSize(1);
    assertThat(telemetryRepo.saved.get(0).metric()).isEqualTo("pressure");
    assertThat(telemetryRepo.saved.get(0).unit()).isEqualTo("bar");
  }

  @Test
  void downstreamExceptionIsSwallowed() {
    // A failure deeper in the pipeline must never propagate out of handle()
    // (a single bad update must not tear down the subscription).
    register(DEVICE_TENANT, "FRIDGE-01");
    telemetryRepo.throwOnSave = true;

    boolean ingested = handler.handle(NODE_TEMP, 1.0, true, null);

    assertThat(ingested).isFalse();
  }

  @Test
  void configuredNodeIdsExposeMappingKeys() {
    assertThat(handler.configuredNodeIds()).containsExactlyInAnyOrder(NODE_TEMP, NODE_PRESS);
  }

  @Test
  void mappingWithBlankNodeIdIsSkippedAtConstruction() {
    OpcUaProperties.NodeMapping blank = mapping("  ", "X", "m", null);
    OpcUaTelemetryMessageHandler h =
        new OpcUaTelemetryMessageHandler(deviceRepo, ingestUseCase, List.of(blank));
    assertThat(h.configuredNodeIds()).isEmpty();
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
