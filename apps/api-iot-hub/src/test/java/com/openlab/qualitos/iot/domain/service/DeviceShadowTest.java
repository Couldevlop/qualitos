package com.openlab.qualitos.iot.domain.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceShadowTest {

  @Test
  void mergeReported_storesLastValuePerMetric() {
    Instant t0 = Instant.parse("2026-06-16T10:00:00Z");
    Map<String, Object> twin = DeviceShadow.mergeReported(Map.of(), "temperature", 4.2, "degC", t0);

    @SuppressWarnings("unchecked")
    Map<String, Object> reported = (Map<String, Object>) twin.get(DeviceShadow.REPORTED);
    @SuppressWarnings("unchecked")
    Map<String, Object> temp = (Map<String, Object>) reported.get("temperature");
    assertThat(temp.get("value")).isEqualTo(4.2);
    assertThat(temp.get("unit")).isEqualTo("degC");
    assertThat(temp.get("at")).isEqualTo(t0.toString());
    assertThat(twin.get(DeviceShadow.LAST_REPORTED_AT)).isEqualTo(t0.toString());
  }

  @Test
  void mergeReported_overwritesSameMetricKeepsOthers() {
    Instant t0 = Instant.parse("2026-06-16T10:00:00Z");
    Instant t1 = Instant.parse("2026-06-16T10:05:00Z");
    Map<String, Object> twin = DeviceShadow.mergeReported(Map.of(), "temperature", 4.2, "degC", t0);
    twin = DeviceShadow.mergeReported(twin, "humidity", 55.0, "pct", t0);
    twin = DeviceShadow.mergeReported(twin, "temperature", 5.0, "degC", t1);

    @SuppressWarnings("unchecked")
    Map<String, Object> reported = (Map<String, Object>) twin.get(DeviceShadow.REPORTED);
    assertThat(reported).containsKeys("temperature", "humidity");
    @SuppressWarnings("unchecked")
    Map<String, Object> temp = (Map<String, Object>) reported.get("temperature");
    assertThat(temp.get("value")).isEqualTo(5.0);
    assertThat(twin.get(DeviceShadow.LAST_REPORTED_AT)).isEqualTo(t1.toString());
  }

  @Test
  void setDesired_replacesDesiredFace() {
    Map<String, Object> twin = DeviceShadow.setDesired(Map.of(), Map.of("setpoint", 4.0));
    @SuppressWarnings("unchecked")
    Map<String, Object> desired = (Map<String, Object>) twin.get(DeviceShadow.DESIRED);
    assertThat(desired.get("setpoint")).isEqualTo(4.0);
  }

  @Test
  void methods_doNotMutateInput() {
    Map<String, Object> original = Map.of();
    DeviceShadow.mergeReported(original, "m", 1.0, "u", Instant.parse("2026-06-16T10:00:00Z"));
    DeviceShadow.setDesired(original, Map.of("k", "v"));
    assertThat(original).isEmpty();
  }
}
