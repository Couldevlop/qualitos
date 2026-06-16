package com.openlab.qualitos.iot.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.iot.domain.model.DeviceType;
import com.openlab.qualitos.iot.domain.model.Protocol;
import com.openlab.qualitos.iot.presentation.dto.DeviceDtos;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Intégration Sparkplug B → télémétrie via le contexte complet (H2, sécurité réelle),
 * connecteur activé par {@code qualitos.iot.sparkplug.enabled=true}.
 *
 * <p>OWASP : 401 sans JWT (A07), tenant résolu depuis le registre device (jamais du
 * payload), 422 quand le device est inconnu/ambigu.
 */
@SpringBootTest(properties = "qualitos.iot.sparkplug.enabled=true")
@ActiveProfiles("test")
@Tag("web")
@Import(SparkplugIngestionControllerIntegrationTest.StubExternalsConfig.class)
class SparkplugIngestionControllerIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;
  private MockMvc mockMvc;

  private MockMvc mvc() {
    if (mockMvc == null) {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    return mockMvc;
  }

  private static String ddata(String deviceCode) {
    return """
        {"groupId":"usine-A","edgeNodeId":"node-1","deviceId":"%s",
         "metrics":[
           {"name":"Temperature","value":4.2,"unit":"degC"},
           {"name":"Vibration","value":1.5,"unit":"g"}
         ],
         "timestamp":1749722400000}""".formatted(deviceCode);
  }

  private void registerDevice(UUID tenant, String code) throws Exception {
    DeviceDtos.RegisterDeviceRequest body = new DeviceDtos.RegisterDeviceRequest(
        code, "Cobot", DeviceType.SENSOR_VIBRATION, Protocol.SPARKPLUG_B,
        "ACME", "Lyon", "Ligne1", "WC-1", "Cobot01", null, null);
    mvc().perform(post("/api/v1/iot/devices")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated());
  }

  @Test
  void unauthenticatedSparkplugPushRejected() throws Exception {
    mvc().perform(post("/api/v1/iot/sparkplug")
            .contentType(MediaType.APPLICATION_JSON)
            .content(ddata("SPB-COBOT-01")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void sparkplugPayloadIsIngestedForKnownDevice() throws Exception {
    UUID tenant = UUID.randomUUID();
    registerDevice(tenant, "SPB-COBOT-02");

    mvc().perform(post("/api/v1/iot/sparkplug")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_GATEWAY")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(ddata("SPB-COBOT-02")))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.ingested").value(2))
        .andExpect(jsonPath("$.dropped").value(0));
  }

  @Test
  void unknownDeviceIs422() throws Exception {
    mvc().perform(post("/api/v1/iot/sparkplug")
            .with(jwt().jwt(b -> b.claim("tenant_id", UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_GATEWAY")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(ddata("SPB-DOES-NOT-EXIST")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.ingested").value(0))
        .andExpect(jsonPath("$.dropped").value(1));
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
