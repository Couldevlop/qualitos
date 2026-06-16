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
 * Intégration Modbus → télémétrie via le contexte complet (H2, sécurité réelle).
 *
 * <p>OWASP : 401 sans JWT (A07), isolation tenant fail-closed (A01), 422 quand tout est
 * rejeté. Connecteur activé via property (désactivé par défaut).
 *
 * <p>Note tenant (§18.2 #2) : le JWT sert l'autorisation par rôle ; le tenant qui fait
 * foi est celui du registre device (résolu par code), JAMAIS le claim ni la charge utile.
 * Le device est enregistré sous le tenant {@code owner} ; un appelant d'un autre tenant
 * peut quand même pousser la lecture (rôle GATEWAY) — la mesure est attribuée au tenant
 * propriétaire du device.
 */
@SpringBootTest(properties = "qualitos.iot.modbus.enabled=true")
@ActiveProfiles("test")
@Tag("web")
@Import(ModbusControllerIntegrationTest.StubExternalsConfig.class)
class ModbusControllerIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;
  private MockMvc mockMvc;

  private MockMvc mvc() {
    if (mockMvc == null) {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    return mockMvc;
  }

  private static String reading(String deviceCode) {
    return """
        {"deviceCode":"%s",
         "readings":[
           {"register":40001,"metric":"pressure","value":4.2,"unit":"bar"},
           {"register":40002,"metric":"temperature","value":55.0,"unit":"degC"}],
         "time":"2026-06-16T08:30:00Z"}""".formatted(deviceCode);
  }

  private void registerDevice(UUID tenant, String code) throws Exception {
    DeviceDtos.RegisterDeviceRequest body = new DeviceDtos.RegisterDeviceRequest(
        code, "Automate atelier", DeviceType.PLC, Protocol.MODBUS_TCP,
        "Usine", "Atelier-3", null, null, null, null, null);
    mvc().perform(post("/api/v1/iot/devices")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated());
  }

  @Test
  void unauthenticatedReadingRejected() throws Exception {
    mvc().perform(post("/api/v1/iot/modbus")
            .contentType(MediaType.APPLICATION_JSON)
            .content(reading("PLC-MODBUS-01")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void readingIsIngestedForKnownDevice() throws Exception {
    UUID tenant = UUID.randomUUID();
    registerDevice(tenant, "PLC-MODBUS-02");

    mvc().perform(post("/api/v1/iot/modbus")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_GATEWAY")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(reading("PLC-MODBUS-02")))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.ingested").value(2))
        .andExpect(jsonPath("$.dropped").value(0));
  }

  @Test
  void unknownDeviceIs422() throws Exception {
    UUID tenant = UUID.randomUUID();

    mvc().perform(post("/api/v1/iot/modbus")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_GATEWAY")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(reading("GHOST-MODBUS")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.ingested").value(0));
  }

  @Test
  void crossTenantCollisionFailsClosed() throws Exception {
    // Même code provisionné sous deux tenants → findUniqueByCode renvoie empty → 422.
    UUID owner = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    registerDevice(owner, "DUP-MODBUS");
    registerDevice(other, "DUP-MODBUS");

    mvc().perform(post("/api/v1/iot/modbus")
            .with(jwt().jwt(b -> b.claim("tenant_id", owner.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_GATEWAY")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(reading("DUP-MODBUS")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.ingested").value(0));
  }

  @Test
  void noValidReadingIs422() throws Exception {
    UUID tenant = UUID.randomUUID();
    registerDevice(tenant, "PLC-MODBUS-05");

    mvc().perform(post("/api/v1/iot/modbus")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_GATEWAY")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"deviceCode":"PLC-MODBUS-05","readings":[{"register":40001,"value":"bad"}]}"""))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.ingested").value(0));
  }

  /** Neutralise l'egress HTTP vers le quality-engine pendant les tests. */
  @TestConfiguration
  static class StubExternalsConfig {
    @Bean
    @Primary
    public com.openlab.qualitos.iot.domain.port.NonConformancePublisher stubPublisher() {
      return event -> {};
    }
  }
}
