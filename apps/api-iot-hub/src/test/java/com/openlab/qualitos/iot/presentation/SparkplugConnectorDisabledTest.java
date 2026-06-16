package com.openlab.qualitos.iot.presentation;

import com.openlab.qualitos.iot.infrastructure.sparkplug.SparkplugIngestionHandler;
import com.openlab.qualitos.iot.presentation.rest.SparkplugIngestionController;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Défaut {@code qualitos.iot.sparkplug.enabled=false} → ni handler ni contrôleur :
 * l'endpoint {@code POST /api/v1/iot/sparkplug} répond 404 (surface nulle, OWASP A05),
 * même garantie que le connecteur MQTT.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
@Import(SparkplugConnectorDisabledTest.StubExternalsConfig.class)
class SparkplugConnectorDisabledTest {

  @Autowired private ApplicationContext context;
  @Autowired private WebApplicationContext webContext;
  private MockMvc mockMvc;

  private MockMvc mvc() {
    if (mockMvc == null) {
      mockMvc = MockMvcBuilders.webAppContextSetup(webContext).apply(springSecurity()).build();
    }
    return mockMvc;
  }

  @Test
  void sparkplugBeansAreAbsentByDefault() {
    assertThat(context.getBeanNamesForType(SparkplugIngestionHandler.class)).isEmpty();
    assertThat(context.getBeanNamesForType(SparkplugIngestionController.class)).isEmpty();
  }

  @Test
  void endpointIs404WhenDisabled() throws Exception {
    mvc().perform(post("/api/v1/iot/sparkplug")
            .with(jwt().jwt(b -> b.claim("tenant_id", UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_GATEWAY")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deviceId\":\"x\",\"metrics\":[{\"name\":\"m\",\"value\":1}]}"))
        .andExpect(status().isNotFound());
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
