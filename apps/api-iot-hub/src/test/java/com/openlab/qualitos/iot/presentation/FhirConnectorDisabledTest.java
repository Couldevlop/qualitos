package com.openlab.qualitos.iot.presentation;

import com.openlab.qualitos.iot.infrastructure.fhir.FhirIngestionHandler;
import com.openlab.qualitos.iot.presentation.rest.FhirIngestionController;
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
 * {@code qualitos.iot.fhir.enabled=false} → ni handler ni contrôleur : la surface
 * d'attaque disparaît entièrement (même garantie que le connecteur MQTT, OWASP A05).
 */
@SpringBootTest(properties = "qualitos.iot.fhir.enabled=false")
@ActiveProfiles("test")
@Tag("web")
@Import(FhirConnectorDisabledTest.StubExternalsConfig.class)
class FhirConnectorDisabledTest {

  @Autowired private ApplicationContext context;

  @Test
  void fhirBeansAreAbsentWhenDisabled() {
    assertThat(context.getBeanNamesForType(FhirIngestionHandler.class)).isEmpty();
    assertThat(context.getBeanNamesForType(FhirIngestionController.class)).isEmpty();
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
