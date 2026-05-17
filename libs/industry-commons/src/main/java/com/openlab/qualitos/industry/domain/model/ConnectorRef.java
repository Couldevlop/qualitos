package com.openlab.qualitos.industry.domain.model;

import java.util.Map;

/**
 * Declarative reference to a native connector. The pack does not contain
 * code — only configuration. Actual adapters live in {@code apps/api-iot-hub}
 * or future {@code connector-*} modules.
 */
public record ConnectorRef(
    String type,           // opc-ua, mqtt, hl7-fhir, lorawan, ...
    String name,
    Map<String, String> config
) {
  public ConnectorRef {
    config = config == null ? Map.of() : Map.copyOf(config);
  }
}
