package com.openlab.qualitos.iot.infrastructure.modbus;

import com.openlab.qualitos.iot.application.usecase.IngestTelemetryUseCase;
import com.openlab.qualitos.iot.domain.port.DeviceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage du connecteur d'ingestion Modbus TCP/RTU (CLAUDE.md §9.4 — équipements legacy).
 *
 * <p>Toute la configuration — et donc chaque bean Modbus — n'est créée que lorsque
 * {@code qualitos.iot.modbus.enabled=true}. Avec le défaut ({@code false}), rien n'est
 * chargé : les tests existants et le déploiement REST-seul ne sont pas affectés et la
 * surface d'attaque disparaît entièrement (même garantie que MQTT/LoRaWAN, OWASP A05).
 */
@Configuration
@EnableConfigurationProperties(ModbusProperties.class)
@ConditionalOnProperty(prefix = "qualitos.iot.modbus", name = "enabled", havingValue = "true")
public class ModbusConnectorConfig {

  @Bean
  public ModbusReadingDecoder modbusReadingDecoder() {
    return new ModbusReadingDecoder();
  }

  @Bean
  public ModbusReadingHandler modbusReadingHandler(
      DeviceRepository deviceRepository,
      IngestTelemetryUseCase ingestUseCase,
      ModbusReadingDecoder decoder,
      ModbusProperties props) {
    return new ModbusReadingHandler(
        deviceRepository, ingestUseCase, decoder, props.getMaxReadings());
  }
}
