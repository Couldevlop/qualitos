package com.openlab.qualitos.iot.domain;

import com.openlab.qualitos.iot.domain.model.Device;
import com.openlab.qualitos.iot.domain.model.DeviceType;
import com.openlab.qualitos.iot.domain.model.Protocol;
import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.model.Threshold;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceModelTest {

  @Test
  void deviceRequiresMandatoryFields() {
    assertThatThrownBy(() -> new Device(
        null, UUID.randomUUID(), "c", "n", DeviceType.PLC, Protocol.OPC_UA,
        null, null, null, null, null, null, null, Instant.now(), null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Device(
        UUID.randomUUID(), null, "c", "n", DeviceType.PLC, Protocol.OPC_UA,
        null, null, null, null, null, null, null, Instant.now(), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void deviceTwinIsImmutable() {
    Map<String, Object> mutable = new java.util.HashMap<>();
    mutable.put("k", "v");
    Device d = new Device(UUID.randomUUID(), UUID.randomUUID(), "c", "n",
        DeviceType.PLC, Protocol.OPC_UA, null, null, null, null, null, null,
        mutable, Instant.now(), null);
    mutable.put("k", "ALTERED");
    assertThat(d.twin()).containsEntry("k", "v");
  }

  @Test
  void telemetryRejectsLongMetric() {
    assertThatThrownBy(() -> new TelemetryPoint(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "x".repeat(101), 1.0, "C", Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void thresholdSeverityInOrder() {
    assertThat(Threshold.Severity.values()).hasSize(3);
  }

  @Test
  void deviceTwinNullDefaultsToEmptyMap() {
    // Covers the `twin == null ? Map.of() : Map.copyOf(twin)` null branch.
    Device d = new Device(UUID.randomUUID(), UUID.randomUUID(), "c", "n",
        DeviceType.PLC, Protocol.OPC_UA, null, null, null, null, null, null,
        null, Instant.now(), null);
    assertThat(d.twin()).isEmpty();
  }

  // ---- Threshold.isBreached branch coverage ------------------------------

  @Test
  void thresholdBreached_onlyMinDefined_belowMin_returnsTrue() {
    Threshold t = new Threshold("temp", 0.0, null, Threshold.Severity.WARNING);
    assertThat(t.isBreached(-1.0)).isTrue();
  }

  @Test
  void thresholdBreached_onlyMinDefined_aboveMin_returnsFalse() {
    Threshold t = new Threshold("temp", 0.0, null, Threshold.Severity.WARNING);
    assertThat(t.isBreached(5.0)).isFalse();
  }

  @Test
  void thresholdBreached_onlyMaxDefined_aboveMax_returnsTrue() {
    Threshold t = new Threshold("temp", null, 100.0, Threshold.Severity.CRITICAL);
    assertThat(t.isBreached(101.0)).isTrue();
  }

  @Test
  void thresholdBreached_bothNull_alwaysFalse() {
    // Covers the both-null short-circuit through to `return false`.
    Threshold t = new Threshold("temp", null, null, Threshold.Severity.INFO);
    assertThat(t.isBreached(42.0)).isFalse();
  }
}
