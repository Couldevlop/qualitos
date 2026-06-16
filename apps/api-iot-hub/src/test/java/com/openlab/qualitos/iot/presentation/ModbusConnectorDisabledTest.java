package com.openlab.qualitos.iot.presentation;

import com.openlab.qualitos.iot.infrastructure.modbus.ModbusReadingHandler;
import com.openlab.qualitos.iot.presentation.rest.ModbusController;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Connecteur Modbus désactivé par défaut → ni handler ni contrôleur : la surface
 * d'attaque est absente sans opt-in (même garantie que MQTT/LoRaWAN, OWASP A05).
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
@Import(ModbusConnectorDisabledTest.StubExternalsConfig.class)
class ModbusConnectorDisabledTest {

  @Autowired private ApplicationContext context;

  @Test
  void modbusBeansAreAbsentByDefault() {
    assertThat(context.getBeanNamesForType(ModbusReadingHandler.class)).isEmpty();
    assertThat(context.getBeanNamesForType(ModbusController.class)).isEmpty();
  }

  @TestConfiguration
  static class StubExternalsConfig {
    @Bean
    @Primary
    public com.openlab.qualitos.iot.domain.port.NonConformancePublisher stubPublisher() {
      return event -> {};
    }
  }
}
