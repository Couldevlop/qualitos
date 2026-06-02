package com.openlab.qualitos.iot.infrastructure.opcua;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the OPC-UA ingestion connector (CLAUDE.md §9.4 — industrial flagship protocol).
 *
 * <p>The whole configuration — and therefore every OPC-UA bean (and the Eclipse Milo client) —
 * is created ONLY when {@code qualitos.iot.opcua.enabled=true}. With the default
 * ({@code false}) nothing is loaded, so existing tests and the REST-only deployment are
 * completely unaffected and no OPC-UA server is required in CI.
 */
@Configuration
@EnableConfigurationProperties(OpcUaProperties.class)
@ConditionalOnProperty(prefix = "qualitos.iot.opcua", name = "enabled", havingValue = "true")
public class OpcUaConnectorConfig {

  @Bean
  public OpcUaTelemetryMessageHandler opcUaTelemetryMessageHandler(
      DeviceRepository deviceRepository,
      IngestTelemetryUseCase ingestUseCase,
      OpcUaProperties props) {
    return new OpcUaTelemetryMessageHandler(deviceRepository, ingestUseCase, props.getNodes());
  }

  @Bean
  public OpcUaIngestionConnector opcUaIngestionConnector(
      OpcUaProperties props, OpcUaTelemetryMessageHandler handler) {
    return new OpcUaIngestionConnector(props, handler);
  }
}
