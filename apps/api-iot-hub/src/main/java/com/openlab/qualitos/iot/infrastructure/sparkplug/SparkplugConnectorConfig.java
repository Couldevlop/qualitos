package com.openlab.qualitos.iot.infrastructure.sparkplug;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage du connecteur d'ingestion Sparkplug B (CLAUDE.md §9.4).
 *
 * <p>La configuration — et donc le handler + le contrôleur Sparkplug — n'est créée que
 * lorsque {@code qualitos.iot.sparkplug.enabled=true}. Avec la valeur par défaut
 * ({@code false}), rien n'est chargé : aucun broker n'est requis en CI et la surface
 * d'attaque reste nulle (OWASP A05), comme pour le connecteur MQTT.
 */
@Configuration
@EnableConfigurationProperties(SparkplugProperties.class)
@ConditionalOnProperty(
    prefix = "qualitos.iot.sparkplug", name = "enabled", havingValue = "true")
public class SparkplugConnectorConfig {

  @Bean
  public SparkplugBDecoder sparkplugBDecoder() {
    return new SparkplugBDecoder();
  }

  @Bean
  public SparkplugIngestionHandler sparkplugIngestionHandler(
      DeviceRepository deviceRepository,
      IngestTelemetryUseCase ingestUseCase,
      SparkplugBDecoder decoder,
      SparkplugProperties props) {
    return new SparkplugIngestionHandler(
        deviceRepository, ingestUseCase, decoder, props.getMaxMetrics());
  }
}
