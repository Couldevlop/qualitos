package com.openlab.qualitos.iot.application;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.application.usecase.RollupTelemetryUseCase;
import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.model.DeviceType;
import com.openlab.qualitos.iot.domain.model.Protocol;
import com.openlab.qualitos.iot.domain.model.RollupBucket;
import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.model.TelemetryRollup;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import com.openlab.qualitos.iot.domain.port.TelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Use case de rollup : portée tenant (anti-IDOR), bornes, défauts — unitaire pur. */
class RollupTelemetryUseCaseTest {

  private static final UUID TENANT = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();

  private InMemoryDeviceRepo deviceRepo;
  private CapturingTelemetryRepo telemetryRepo;
  private RollupTelemetryUseCase useCase;

  @BeforeEach
  void setUp() {
    deviceRepo = new InMemoryDeviceRepo();
    telemetryRepo = new CapturingTelemetryRepo();
    useCase = new RollupTelemetryUseCase(deviceRepo, telemetryRepo);
  }

  private Device register(UUID tenant, String code) {
    Device d = new Device(UUID.randomUUID(), tenant, code, "Dev",
        DeviceType.SENSOR_TEMPERATURE, Protocol.MQTT,
        null, null, null, null, null, null, Map.of(), Instant.now(), null);
    return deviceRepo.save(d);
  }

  @Test
  @DisplayName("Device d'un autre tenant → 404 fail-closed (anti-IDOR A01)")
  void crossTenantDeviceNotFound() {
    Device d = register(OTHER, "X");
    assertThatThrownBy(() -> useCase.rollup(TENANT, d.id(), "temp", RollupBucket.HOUR, 10))
        .isInstanceOf(IngestTelemetryUseCase.DeviceNotFoundException.class);
  }

  @Test
  void metricIsRequired() {
    Device d = register(TENANT, "X");
    assertThatThrownBy(() -> useCase.rollup(TENANT, d.id(), " ", RollupBucket.HOUR, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Limite null → défaut 168 ; limite excessive → bornée à MAX_LIMIT")
  void limitsAreClamped() {
    Device d = register(TENANT, "X");

    useCase.rollup(TENANT, d.id(), "temp", RollupBucket.HOUR, null);
    assertThat(telemetryRepo.lastLimit).isEqualTo(168);

    useCase.rollup(TENANT, d.id(), "temp", RollupBucket.HOUR, 99_999);
    assertThat(telemetryRepo.lastLimit).isEqualTo(RollupTelemetryUseCase.MAX_LIMIT);

    useCase.rollup(TENANT, d.id(), "temp", RollupBucket.HOUR, 0);
    assertThat(telemetryRepo.lastLimit).isEqualTo(168);
  }

  @Test
  @DisplayName("Bucket null → HOUR par défaut, délégué au repository")
  void defaultBucketIsHour() {
    Device d = register(TENANT, "X");
    useCase.rollup(TENANT, d.id(), "temp", null, 5);
    assertThat(telemetryRepo.lastBucket).isEqualTo(RollupBucket.HOUR);
  }

  @Test
  void delegatesBucketsFromRepository() {
    Device d = register(TENANT, "X");
    telemetryRepo.toReturn.set(List.of(
        new TelemetryRollup(Instant.parse("2026-06-12T10:00:00Z"), "temp", 5.0, 4.0, 6.0, 3)));

    var out = useCase.rollup(TENANT, d.id(), "temp", RollupBucket.HOUR, 10);

    assertThat(out).hasSize(1);
    assertThat(out.get(0).avg()).isEqualTo(5.0);
  }

  // ---- stubs -----------------------------------------------------------------

  static class InMemoryDeviceRepo implements DeviceRepository {
    final Map<UUID, Device> byId = new LinkedHashMap<>();
    @Override public Device save(Device d) { byId.put(d.id(), d); return d; }
    @Override public Optional<Device> findById(UUID t, UUID i) {
      Device d = byId.get(i);
      return d != null && d.tenantId().equals(t) ? Optional.of(d) : Optional.empty();
    }
    @Override public Optional<Device> findByCode(UUID t, String c) { return Optional.empty(); }
    @Override public Optional<Device> findUniqueByCode(String c) { return Optional.empty(); }
    @Override public List<Device> findAllByTenant(UUID t) { return List.of(); }
    @Override public void touchLastSeen(UUID t, UUID id, Instant when) { }
    @Override public long countByTenant(UUID t) { return 0; }
    @Override public void updateTwin(UUID t, UUID id, Map<String, Object> twin) { }
  }

  static class CapturingTelemetryRepo implements TelemetryRepository {
    int lastLimit;
    RollupBucket lastBucket;
    final AtomicReference<List<TelemetryRollup>> toReturn = new AtomicReference<>(List.of());

    @Override public TelemetryPoint save(TelemetryPoint p) { return p; }
    @Override public List<TelemetryPoint> saveAll(List<TelemetryPoint> pts) { return pts; }
    @Override public List<TelemetryPoint> findByDevice(UUID t, UUID d, Instant f, Instant to, int l) {
      return List.of();
    }
    @Override public long countByTenant(UUID t) { return 0; }
    @Override public List<TelemetryRollup> rollupByDevice(
        UUID t, UUID d, String metric, RollupBucket bucket, int limit) {
      this.lastLimit = limit;
      this.lastBucket = bucket;
      return toReturn.get();
    }
  }
}
