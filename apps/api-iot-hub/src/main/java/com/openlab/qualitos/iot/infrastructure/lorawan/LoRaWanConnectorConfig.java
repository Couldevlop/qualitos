package com.openlab.qualitos.iot.infrastructure.lorawan;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage du connecteur d'ingestion LoRaWAN (CLAUDE.md §9.4 — TTN / ChirpStack).
 *
 * <p>Toute la configuration — et donc chaque bean LoRaWAN — n'est créée que lorsque
 * {@code qualitos.iot.lorawan.enabled=true}. Avec le défaut ({@code false}), rien n'est
 * chargé : les tests existants et le déploiement REST-seul ne sont pas affectés et la
 * surface d'attaque disparaît entièrement (même garantie que MQTT, OWASP A05).
 */
@Configuration
@EnableConfigurationProperties(LoRaWanProperties.class)
@ConditionalOnProperty(prefix = "qualitos.iot.lorawan", name = "enabled", havingValue = "true")
public class LoRaWanConnectorConfig {

  @Bean
  public LoRaWanUplinkDecoder loRaWanUplinkDecoder() {
    return new LoRaWanUplinkDecoder();
  }

  @Bean
  public LoRaWanUplinkHandler loRaWanUplinkHandler(
      DeviceRepository deviceRepository,
      IngestTelemetryUseCase ingestUseCase,
      LoRaWanUplinkDecoder decoder,
      LoRaWanProperties props) {
    return new LoRaWanUplinkHandler(
        deviceRepository, ingestUseCase, decoder,
        props.getDeviceIdField(), props.getMaxMeasurements());
  }
}
