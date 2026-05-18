package com.openlab.qualitos.iot.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.iot.domain.model.DeviceType;
import com.openlab.qualitos.iot.domain.model.Protocol;
import com.openlab.qualitos.iot.presentation.dto.DeviceDtos;
import com.openlab.qualitos.iot.presentation.dto.TelemetryDtos;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
@Import(DeviceControllerIntegrationTest.StubExternalsConfig.class)
class DeviceControllerIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;
  private MockMvc mockMvc;

  private MockMvc mvc() {
    if (mockMvc == null) {
      mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    return mockMvc;
  }

  @Test
  void unauthenticatedRequestRejected() throws Exception {
    mvc().perform(get("/api/v1/iot/devices"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void healthIsPublic() throws Exception {
    mvc().perform(get("/actuator/health"))
        .andExpect(status().isOk());
  }

  @Test
  void registerThenListDevice() throws Exception {
    UUID tenant = UUID.randomUUID();
    DeviceDtos.RegisterDeviceRequest body = new DeviceDtos.RegisterDeviceRequest(
        "PLC-001", "Press 1", DeviceType.PLC, Protocol.OPC_UA,
        "ACME", "Bordeaux", "AssemblyLine1", "WC-3", "Press01",
        null, null);

    MvcResult created = mvc().perform(post("/api/v1/iot/devices")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("PLC-001"))
        .andExpect(jsonPath("$.tenantId").value(tenant.toString()))
        .andReturn();
    assertThat(created.getResponse().getHeader("Location")).contains("/api/v1/iot/devices/");

    mvc().perform(get("/api/v1/iot/devices")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].code").value("PLC-001"));
  }

  @Test
  void duplicateCodeReturns409ProblemDetail() throws Exception {
    UUID tenant = UUID.randomUUID();
    DeviceDtos.RegisterDeviceRequest body = new DeviceDtos.RegisterDeviceRequest(
        "DUP-1", "Dup", DeviceType.SENSOR_TEMPERATURE, Protocol.MQTT,
        null, null, null, null, null, null, null);
    var jwtMutator = jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));
    mvc().perform(post("/api/v1/iot/devices").with(jwtMutator)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated());
    mvc().perform(post("/api/v1/iot/devices").with(jwtMutator)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type").value("https://qualitos.local/errors/device-already-exists"));
  }

  @Test
  void invalidPayloadReturnsValidationProblem() throws Exception {
    UUID tenant = UUID.randomUUID();
    // Missing code, name etc.
    String badJson = "{\"type\":\"PLC\",\"protocol\":\"OPC_UA\"}";
    mvc().perform(post("/api/v1/iot/devices")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TENANT_ADMIN")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(badJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Validation failed"));
  }

  @Test
  void ingestTelemetryRequiresKnownDevice() throws Exception {
    UUID tenant = UUID.randomUUID();
    UUID phantom = UUID.randomUUID();
    var body = new TelemetryDtos.IngestRequest(phantom, "temp", 5.0, "C", null);
    mvc().perform(post("/api/v1/iot/telemetry")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_DEVICE")))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("https://qualitos.local/errors/device-not-found"));
  }

  @Test
  void ingestTelemetryHappyPath() throws Exception {
    UUID tenant = UUID.randomUUID();
    var reg = jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));

    DeviceDtos.RegisterDeviceRequest body = new DeviceDtos.RegisterDeviceRequest(
        "MQTT-1", "Sensor", DeviceType.SENSOR_TEMPERATURE, Protocol.MQTT,
        null, null, null, null, null, null, null);
    MvcResult result = mvc().perform(post("/api/v1/iot/devices").with(reg)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated()).andReturn();
    DeviceDtos.DeviceResponse dev = objectMapper.readValue(
        result.getResponse().getContentAsByteArray(), DeviceDtos.DeviceResponse.class);

    var ingestBody = new TelemetryDtos.IngestRequest(dev.id(), "temp", 6.5, "C", null);
    mvc().perform(post("/api/v1/iot/telemetry").with(reg)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ingestBody)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.metric").value("temp"));
  }

  @Test
  void ingestBatchHappyPath() throws Exception {
    UUID tenant = UUID.randomUUID();
    var reg = jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));
    DeviceDtos.RegisterDeviceRequest body = new DeviceDtos.RegisterDeviceRequest(
        "BATCH-1", "Sensor", DeviceType.SENSOR_VIBRATION, Protocol.MQTT,
        null, null, null, null, null, null, null);
    MvcResult result = mvc().perform(post("/api/v1/iot/devices").with(reg)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated()).andReturn();
    DeviceDtos.DeviceResponse dev = objectMapper.readValue(
        result.getResponse().getContentAsByteArray(), DeviceDtos.DeviceResponse.class);

    var batch = new TelemetryDtos.BatchIngestRequest(dev.id(), List.of(
        new TelemetryDtos.PointEntry("rms", 1.2, "g", null),
        new TelemetryDtos.PointEntry("rms", 1.3, "g", null),
        new TelemetryDtos.PointEntry("rms", 1.4, "g", null)));
    mvc().perform(post("/api/v1/iot/telemetry/batch").with(reg)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(batch)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  void tenantIsolationOnList() throws Exception {
    UUID alice = UUID.randomUUID();
    UUID bob = UUID.randomUUID();
    var aliceJwt = jwt().jwt(b -> b.claim("tenant_id", alice.toString()))
        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));
    var bobJwt = jwt().jwt(b -> b.claim("tenant_id", bob.toString()));

    DeviceDtos.RegisterDeviceRequest body = new DeviceDtos.RegisterDeviceRequest(
        "ISO-1", "A device", DeviceType.PLC, Protocol.OPC_UA,
        null, null, null, null, null, null, null);
    mvc().perform(post("/api/v1/iot/devices").with(aliceJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated());

    mvc().perform(get("/api/v1/iot/devices").with(bobJwt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  /** Stub out the egress publisher so no real HTTP call is attempted in tests. */
  @TestConfiguration
  static class StubExternalsConfig {
    @Bean
    @Primary
    public com.openlab.qualitos.iot.domain.port.NonConformancePublisher stubPublisher() {
      return event -> {};
    }
  }
}
