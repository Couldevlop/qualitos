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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
  void deviceShadowRoundTripAndReportedState() throws Exception {
    UUID tenant = UUID.randomUUID();
    var reg = jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));

    // Register with an initial twin (desired setpoint) → JSON round-trip must persist it.
    DeviceDtos.RegisterDeviceRequest body = new DeviceDtos.RegisterDeviceRequest(
        "FRIDGE-1", "Pharma fridge", DeviceType.SENSOR_TEMPERATURE, Protocol.MQTT,
        null, null, null, null, null, null,
        java.util.Map.of("desired", java.util.Map.of("setpoint", 4.0)));
    MvcResult result = mvc().perform(post("/api/v1/iot/devices").with(reg)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated()).andReturn();
    DeviceDtos.DeviceResponse dev = objectMapper.readValue(
        result.getResponse().getContentAsByteArray(), DeviceDtos.DeviceResponse.class);

    // Twin rehydrated from JSON (not empty) — the documented stub is fixed.
    mvc().perform(get("/api/v1/iot/devices/" + dev.id() + "/shadow").with(reg))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.desired.setpoint").value(4.0));

    // Ingest telemetry → the reported face of the shadow reflects the last value.
    var ingestBody = new TelemetryDtos.IngestRequest(dev.id(), "temperature", 8.7, "degC", null);
    mvc().perform(post("/api/v1/iot/telemetry").with(reg)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ingestBody)))
        .andExpect(status().isAccepted());

    mvc().perform(get("/api/v1/iot/devices/" + dev.id() + "/shadow").with(reg))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reported.temperature.value").value(8.7))
        .andExpect(jsonPath("$.reported.temperature.unit").value("degC"))
        .andExpect(jsonPath("$.desired.setpoint").value(4.0));  // preserved

    // PATCH desired updates only the desired face.
    mvc().perform(patch("/api/v1/iot/devices/" + dev.id() + "/shadow/desired").with(reg)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"setpoint\":2.0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.desired.setpoint").value(2.0))
        .andExpect(jsonPath("$.reported.temperature.value").value(8.7));  // reported untouched
  }

  @Test
  void telemetryRollupAggregatesByHourBucket() throws Exception {
    UUID tenant = UUID.randomUUID();
    var reg = jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))
        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));

    DeviceDtos.RegisterDeviceRequest body = new DeviceDtos.RegisterDeviceRequest(
        "ROLLUP-1", "Sensor", DeviceType.SENSOR_TEMPERATURE, Protocol.MQTT,
        null, null, null, null, null, null, null);
    MvcResult result = mvc().perform(post("/api/v1/iot/devices").with(reg)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated()).andReturn();
    UUID deviceId = objectMapper.readValue(
        result.getResponse().getContentAsByteArray(), DeviceDtos.DeviceResponse.class).id();

    // Deux points dans la tranche 10h (avg=6, min=4, max=8), un point dans la tranche 11h.
    ingest(reg, deviceId, "temp", 4.0, "2026-06-12T10:05:00Z");
    ingest(reg, deviceId, "temp", 8.0, "2026-06-12T10:55:00Z");
    ingest(reg, deviceId, "temp", 5.0, "2026-06-12T11:30:00Z");
    // Une autre métrique ne doit PAS polluer le rollup de "temp".
    ingest(reg, deviceId, "pressure", 99.0, "2026-06-12T10:10:00Z");

    mvc().perform(get("/api/v1/iot/devices/" + deviceId + "/telemetry/rollup")
            .param("metric", "temp").param("bucket", "hour").with(reg))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        // tri DESC : le bucket 11h en premier.
        .andExpect(jsonPath("$[0].count").value(1))
        .andExpect(jsonPath("$[0].avg").value(5.0))
        .andExpect(jsonPath("$[1].count").value(2))
        .andExpect(jsonPath("$[1].avg").value(6.0))
        .andExpect(jsonPath("$[1].min").value(4.0))
        .andExpect(jsonPath("$[1].max").value(8.0));
  }

  @Test
  void rollupUnknownDeviceIs404() throws Exception {
    UUID tenant = UUID.randomUUID();
    mvc().perform(get("/api/v1/iot/devices/" + UUID.randomUUID() + "/telemetry/rollup")
            .param("metric", "temp")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))))
        .andExpect(status().isNotFound());
  }

  @Test
  void rollupBadBucketIs400() throws Exception {
    UUID tenant = UUID.randomUUID();
    mvc().perform(get("/api/v1/iot/devices/" + UUID.randomUUID() + "/telemetry/rollup")
            .param("metric", "temp").param("bucket", "fortnight")
            .with(jwt().jwt(b -> b.claim("tenant_id", tenant.toString()))))
        .andExpect(status().isBadRequest());
  }

  private void ingest(
      org.springframework.test.web.servlet.request.RequestPostProcessor reg,
      UUID deviceId, String metric, double value, String recordedAtIso) throws Exception {
    var ingestBody = new TelemetryDtos.IngestRequest(
        deviceId, metric, value, "C", Instant.parse(recordedAtIso));
    mvc().perform(post("/api/v1/iot/telemetry").with(reg)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ingestBody)))
        .andExpect(status().isAccepted());
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
