package com.openlab.qualitos.iot.infrastructure.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the MQTT ingestion connector (CLAUDE.md §9.4).
 *
 * <p>The whole configuration — and therefore every MQTT bean — is created ONLY when
 * {@code qualitos.iot.mqtt.enabled=true}. With the default ({@code false}) nothing is
 * loaded, so existing tests and the REST-only deployment are completely unaffected and
 * no broker is required in CI.
 */
@Configuration
@EnableConfigurationProperties(MqttProperties.class)
@ConditionalOnProperty(prefix = "qualitos.iot.mqtt", name = "enabled", havingValue = "true")
public class MqttConnectorConfig {

  @Bean
  public MqttTelemetryMessageHandler mqttTelemetryMessageHandler(
      DeviceRepository deviceRepository,
      IngestTelemetryUseCase ingestUseCase,
      ObjectMapper objectMapper) {
    return new MqttTelemetryMessageHandler(deviceRepository, ingestUseCase, objectMapper);
  }

  @Bean
  public MqttIngestionConnector mqttIngestionConnector(
      MqttProperties props, MqttTelemetryMessageHandler handler) {
    return new MqttIngestionConnector(props, handler);
  }
}
