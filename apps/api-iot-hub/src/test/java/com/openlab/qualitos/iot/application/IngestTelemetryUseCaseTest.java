package com.openlab.qualitos.iot.application;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.application.usecase.RegisterDeviceUseCase;
import com.openlab.qualitos.iot.domain.model.*;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import com.openlab.qualitos.iot.domain.port.NonConformancePublisher;
import com.openlab.qualitos.iot.domain.port.TelemetryRepository;
import com.openlab.qualitos.iot.domain.port.ThresholdRegistry;
import com.openlab.qualitos.iot.domain.service.StreamRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IngestTelemetryUseCaseTest {

  private InMemoryDeviceRepo deviceRepo;
  private InMemoryTelemetryRepo telemetryRepo;
  private RecordingPublisher publisher;
  private List<Threshold> thresholds;
  private IngestTelemetryUseCase useCase;
  private RegisterDeviceUseCase registerUseCase;

  @BeforeEach
  void setUp() {
    deviceRepo = new InMemoryDeviceRepo();
    telemetryRepo = new InMemoryTelemetryRepo();
    publisher = new RecordingPublisher();
    thresholds = new ArrayList<>();
    ThresholdRegistry registry = (tid, did, m) -> thresholds;
    useCase = new IngestTelemetryUseCase(deviceRepo, telemetryRepo, new StreamRuleEngine(registry), publisher);
    registerUseCase = new RegisterDeviceUseCase(deviceRepo);
  }

  @Test
  void registersAndIngests() {
    UUID tenant = UUID.randomUUID();
    Device d = registerUseCase.register(tenant, "DEV1", "Device 1",
        DeviceType.SENSOR_TEMPERATURE, Protocol.MQTT,
        null, null, null, null, null, null, Map.of());

    var saved = useCase.ingest(tenant, d.id(), "temp", 7.2, "C", Instant.now());
    assertThat(saved.metric()).isEqualTo("temp");
    assertThat(telemetryRepo.all).hasSize(1);
    assertThat(deviceRepo.lastSeenTouched).isTrue();
  }

  @Test
  void rejectsUnknownDevice() {
    assertThatThrownBy(() -> useCase.ingest(UUID.randomUUID(), UUID.randomUUID(), "m", 1.0, null, Instant.now()))
        .isInstanceOf(IngestTelemetryUseCase.DeviceNotFoundException.class);
  }

  @Test
  void breachTriggersPublisher() {
    UUID tenant = UUID.randomUUID();
    Device d = registerUseCase.register(tenant, "DEV2", "X",
        DeviceType.SENSOR_TEMPERATURE, Protocol.MQTT,
        null, null, null, null, null, null, Map.of());
    thresholds.add(new Threshold("temp", 0.0, 8.0, Threshold.Severity.CRITICAL));

    useCase.ingest(tenant, d.id(), "temp", 12.0, "C", Instant.now());
    assertThat(publisher.events).hasSize(1);
    assertThat(publisher.events.get(0).observedValue()).isEqualTo(12.0);
  }

  @Test
  void duplicateDeviceCodeRejected() {
    UUID tenant = UUID.randomUUID();
    registerUseCase.register(tenant, "DUP", "x", DeviceType.PLC, Protocol.OPC_UA,
        null, null, null, null, null, null, null);
    assertThatThrownBy(() -> registerUseCase.register(tenant, "DUP", "x", DeviceType.PLC, Protocol.OPC_UA,
        null, null, null, null, null, null, null))
        .isInstanceOf(RegisterDeviceUseCase.DeviceAlreadyExistsException.class);
  }

  @Test
  void batchIngest() {
    UUID tenant = UUID.randomUUID();
    Device d = registerUseCase.register(tenant, "DEV3", "X", DeviceType.PLC, Protocol.OPC_UA,
        null, null, null, null, null, null, null);
    List<TelemetryPoint> batch = List.of(
        new TelemetryPoint(null, tenant, d.id(), "rpm", 1500.0, null, Instant.now()),
        new TelemetryPoint(null, tenant, d.id(), "rpm", 1505.0, null, Instant.now()));
    var saved = useCase.ingestBatch(tenant, d.id(), batch);
    assertThat(saved).hasSize(2);
  }

  @Test
  void batchTooLargeRejected() {
    UUID tenant = UUID.randomUUID();
    Device d = registerUseCase.register(tenant, "DEV4", "X", DeviceType.PLC, Protocol.OPC_UA,
        null, null, null, null, null, null, null);
    List<TelemetryPoint> huge = new ArrayList<>();
    for (int i = 0; i < 1001; i++) {
      huge.add(new TelemetryPoint(null, tenant, d.id(), "rpm", (double) i, null, Instant.now()));
    }
    assertThatThrownBy(() -> useCase.ingestBatch(tenant, d.id(), huge))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void emptyBatchReturnsEmpty() {
    UUID tenant = UUID.randomUUID();
    Device d = registerUseCase.register(tenant, "DEV5", "X", DeviceType.PLC, Protocol.OPC_UA,
        null, null, null, null, null, null, null);
    assertThat(useCase.ingestBatch(tenant, d.id(), List.of())).isEmpty();
    assertThat(useCase.ingestBatch(tenant, d.id(), null)).isEmpty();
  }

  @Test
  void crossTenantIsolation() {
    UUID alice = UUID.randomUUID();
    UUID bob = UUID.randomUUID();
    Device d = registerUseCase.register(alice, "DEV6", "X", DeviceType.PLC, Protocol.OPC_UA,
        null, null, null, null, null, null, null);
    // bob tries to ingest into alice's device -> NotFound (no cross-tenant leakage)
    assertThatThrownBy(() -> useCase.ingest(bob, d.id(), "m", 1.0, null, Instant.now()))
        .isInstanceOf(IngestTelemetryUseCase.DeviceNotFoundException.class);
  }

  // ---- stubs ----------------------------------------------------------

  static class InMemoryDeviceRepo implements DeviceRepository {
    final Map<UUID, Device> byId = new LinkedHashMap<>();
    boolean lastSeenTouched = false;

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
    @Override public void touchLastSeen(UUID t, UUID id, Instant when) { lastSeenTouched = true; }
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

  static class InMemoryTelemetryRepo implements TelemetryRepository {
    final List<TelemetryPoint> all = new ArrayList<>();
    @Override public TelemetryPoint save(TelemetryPoint p) { all.add(p); return p; }
    @Override public List<TelemetryPoint> saveAll(List<TelemetryPoint> pts) { all.addAll(pts); return pts; }
    @Override public List<TelemetryPoint> findByDevice(UUID t, UUID d, Instant from, Instant to, int lim) {
      return all.stream().filter(p -> p.tenantId().equals(t) && p.deviceId().equals(d)).toList();
    }
    @Override public long countByTenant(UUID t) { return all.stream().filter(p -> p.tenantId().equals(t)).count(); }
  }

  static class RecordingPublisher implements NonConformancePublisher {
    final List<ThresholdBreachEvent> events = new ArrayList<>();
    @Override public void notifyBreach(ThresholdBreachEvent e) { events.add(e); }
  }
}
