package com.openlab.qualitos.iot.domain;

import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.model.Threshold;
import com.openlab.qualitos.iot.domain.port.ThresholdRegistry;
import com.openlab.qualitos.iot.domain.service.StreamRuleEngine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StreamRuleEngineTest {

  private final UUID tenant = UUID.randomUUID();
  private final UUID device = UUID.randomUUID();

  @Test
  void breachAboveMax() {
    Threshold t = new Threshold("temp", null, 8.0, Threshold.Severity.CRITICAL);
    var engine = new StreamRuleEngine((tid, did, m) -> List.of(t));
    var point = new TelemetryPoint(UUID.randomUUID(), tenant, device, "temp", 10.5, "C", Instant.now());
    assertThat(engine.evaluate(point))
        .singleElement()
        .satisfies(b -> assertThat(b.observedValue()).isEqualTo(10.5));
  }

  @Test
  void breachBelowMin() {
    Threshold t = new Threshold("temp", 2.0, null, Threshold.Severity.WARNING);
    var engine = new StreamRuleEngine((tid, did, m) -> List.of(t));
    var point = new TelemetryPoint(UUID.randomUUID(), tenant, device, "temp", 1.0, "C", Instant.now());
    assertThat(engine.evaluate(point)).hasSize(1);
  }

  @Test
  void noBreachWithinRange() {
    Threshold t = new Threshold("temp", 2.0, 8.0, Threshold.Severity.WARNING);
    var engine = new StreamRuleEngine((tid, did, m) -> List.of(t));
    var point = new TelemetryPoint(UUID.randomUUID(), tenant, device, "temp", 5.0, "C", Instant.now());
    assertThat(engine.evaluate(point)).isEmpty();
  }

  @Test
  void noBreachIfNoRules() {
    var engine = new StreamRuleEngine((tid, did, m) -> List.of());
    var point = new TelemetryPoint(UUID.randomUUID(), tenant, device, "temp", 100.0, "C", Instant.now());
    assertThat(engine.evaluate(point)).isEmpty();
  }

  @Test
  void noBreachOnNullValue() {
    var engine = new StreamRuleEngine((tid, did, m) -> List.of(
        new Threshold("x", 0.0, 1.0, Threshold.Severity.INFO)));
    var point = new TelemetryPoint(UUID.randomUUID(), tenant, device, "x", null, null, Instant.now());
    assertThat(engine.evaluate(point)).isEmpty();
  }

  @Test
  void multipleBreaches() {
    var registry = (ThresholdRegistry) (tid, did, m) -> List.of(
        new Threshold("p", 0.0, 10.0, Threshold.Severity.WARNING),
        new Threshold("p", 0.0, 5.0, Threshold.Severity.CRITICAL));
    var engine = new StreamRuleEngine(registry);
    var point = new TelemetryPoint(UUID.randomUUID(), tenant, device, "p", 11.0, null, Instant.now());
    assertThat(engine.evaluate(point)).hasSize(2);
  }

  @Test
  void rejectsNullRegistry() {
    assertThatThrownBy(() -> new StreamRuleEngine(null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
