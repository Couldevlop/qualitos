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
 * Intégration FHIR → télémétrie via le contexte complet (H2, sécurité réelle).
 *
 * <p>OWASP : 401 sans JWT (A07), isolation tenant fail-closed (A01), 422 quand
 * tout est rejeté.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
@Import(FhirIngestionControllerIntegrationTest.StubExternalsConfig.class)
class FhirIngestionControllerIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;
  private MockMvc mockMvc;

  private MockMvc mvc() {
    if (mockMvc == null) {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    return mockMvc;
  }

  private static String fhirObservation(String deviceCode, double value) {
    return """
        {"resourceType":"Observation",
         "status":"final",
         "device":{"identifier":{"value":"%s"}},
         "code":{"coding":[{"system":"http://loinc.org","code":"8310-5"}]},
         "valueQuantity":{"value":%s,"unit":"Cel"},
         "effectiveDateTime":"2026-06-04T10:00:00Z"}""".formatted(deviceCode, value);
  }

  private void registerDevice(UUID tenant, String code) throws Exception {
    DeviceDtos.RegisterDeviceRequest body = new DeviceDtos.RegisterDeviceRequest(
        code, "Frigo pharmacie", DeviceType.BIOMED, Protocol.HL7_FHIR,
        "CHU", "Bordeaux", "Pharmacie", null, null, null, null);
    mvc().perform(post("/api/v1/iot/devices")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated());
  }

  @Test
  void unauthenticatedFhirPushRejected() throws Exception {
    mvc().perform(post("/api/v1/iot/fhir")
            .contentType("application/fhir+json")
            .content(fhirObservation("FRIDGE-FHIR-01", 7.0)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void fhirObservationIsIngestedForKnownDevice() throws Exception {
    UUID tenant = UUID.randomUUID();
    registerDevice(tenant, "FRIDGE-FHIR-02");

    mvc().perform(post("/api/v1/iot/fhir")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_GATEWAY")))
            .contentType("application/fhir+json")
            .content(fhirObservation("FRIDGE-FHIR-02", 7.4)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.ingested").value(1))
        .andExpect(jsonPath("$.dropped").value(0));
  }

  @Test
  void bundleIsIngested() throws Exception {
    UUID tenant = UUID.randomUUID();
    registerDevice(tenant, "FRIDGE-FHIR-03");

    String bundle = """
        {"resourceType":"Bundle","type":"collection","entry":[
          {"resource":%s},
          {"resource":{"resourceType":"Patient","id":"ignored"}},
          {"resource":%s}
        ]}""".formatted(
        fhirObservation("FRIDGE-FHIR-03", 6.8), fhirObservation("FRIDGE-FHIR-03", 7.1));

    mvc().perform(post("/api/v1/iot/fhir")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_GATEWAY")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(bundle))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.ingested").value(2));
  }

  @Test
  void crossTenantDeviceIsInvisible_failClosed() throws Exception {
    UUID owner = UUID.randomUUID();
    UUID attacker = UUID.randomUUID();
    registerDevice(owner, "FRIDGE-FHIR-04");

    // Le device existe… pour un AUTRE tenant : rejet 422 (anti-IDOR A01).
    mvc().perform(post("/api/v1/iot/fhir")
            .with(jwt().jwt(b -> b.claim("tenant_id", attacker.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_GATEWAY")))
            .contentType("application/fhir+json")
            .content(fhirObservation("FRIDGE-FHIR-04", 7.0)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.ingested").value(0))
        .andExpect(jsonPath("$.dropped").value(1));
  }

  @Test
  void unsupportedResourceTypeIs422() throws Exception {
    mvc().perform(post("/api/v1/iot/fhir")
            .with(jwt().jwt(b -> b.claim("tenant_id", UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_GATEWAY")))
            .contentType("application/fhir+json")
            .content("""
                {"resourceType":"Patient","id":"p1"}"""))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.issues[0]").value(
            org.hamcrest.Matchers.containsString("unsupported resourceType")));
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
