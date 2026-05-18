package com.openlab.qualitos.iot.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring-published event when a {@link Threshold} is breached. Consumed by
 * the {@code infrastructure/external/QualityEngineClient} to create a NC
 * via Feign call to api-quality-engine.
 */
public record ThresholdBreachEvent(
    UUID tenantId,
    UUID deviceId,
    String metric,
    double observedValue,
    Threshold rule,
    Instant occurredAt
) {}
