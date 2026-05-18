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
}
