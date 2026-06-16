package com.openlab.qualitos.iot.presentation;

import com.openlab.qualitos.iot.infrastructure.lorawan.LoRaWanUplinkHandler;
import com.openlab.qualitos.iot.presentation.rest.LoRaWanController;
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
 * Connecteur LoRaWAN désactivé par défaut → ni handler ni contrôleur : la surface
 * d'attaque est absente sans opt-in (même garantie que MQTT, OWASP A05).
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
@Import(LoRaWanConnectorDisabledTest.StubExternalsConfig.class)
class LoRaWanConnectorDisabledTest {

  @Autowired private ApplicationContext context;

  @Test
  void loRaWanBeansAreAbsentByDefault() {
    assertThat(context.getBeanNamesForType(LoRaWanUplinkHandler.class)).isEmpty();
    assertThat(context.getBeanNamesForType(LoRaWanController.class)).isEmpty();
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
