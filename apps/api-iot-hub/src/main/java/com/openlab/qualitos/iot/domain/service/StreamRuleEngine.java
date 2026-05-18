package com.openlab.qualitos.iot.domain.service;

import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.model.Threshold;
import com.openlab.qualitos.iot.domain.model.ThresholdBreachEvent;
import com.openlab.qualitos.iot.domain.port.ThresholdRegistry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-domain rule engine: given a telemetry point and the registry of
 * thresholds, return zero or more {@link ThresholdBreachEvent}. No I/O,
 * no Spring — fully unit-testable.
 */
public final class StreamRuleEngine {

  private final ThresholdRegistry registry;

  public StreamRuleEngine(ThresholdRegistry registry) {
    if (registry == null) throw new IllegalArgumentException("registry");
    this.registry = registry;
  }

  public List<ThresholdBreachEvent> evaluate(TelemetryPoint point) {
    if (point == null || point.value() == null) return List.of();
    List<Threshold> applicable = registry.findFor(point.tenantId(), point.deviceId(), point.metric());
    if (applicable == null || applicable.isEmpty()) return List.of();

    List<ThresholdBreachEvent> events = new ArrayList<>();
    for (Threshold t : applicable) {
      if (t.isBreached(point.value())) {
        events.add(new ThresholdBreachEvent(
            point.tenantId(),
            point.deviceId(),
            point.metric(),
            point.value(),
            t,
            Instant.now()));
      }
    }
    return events;
  }
}
