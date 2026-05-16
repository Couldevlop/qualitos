package com.openlab.qualitos.quality.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = IotController.class)
class IotControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean IotDeviceService deviceService;
    @MockitoBean TelemetryIngestionService telemetryService;
    ObjectMapper om;

    static final UUID DEV = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(deviceService.list(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deviceResp())));
        mockMvc.perform(get("/api/v1/iot/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DEV.toString()));
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(deviceService.create(any())).thenReturn(deviceResp());
        IotDto.CreateDeviceRequest req = new IotDto.CreateDeviceRequest(
                "press-A", "Press A", IotDeviceType.PLC, IotProtocol.OPC_UA,
                "site|line", null, null, USER);
        mockMvc.perform(post("/api/v1/iot/devices").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_invalidCode_returns400() throws Exception {
        String body = "{\"code\":\"bad code with spaces\",\"name\":\"n\",\"deviceType\":\"PLC\","
                + "\"protocol\":\"OPC_UA\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/iot/devices").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingProtocol_returns400() throws Exception {
        String body = "{\"code\":\"c\",\"name\":\"n\",\"deviceType\":\"PLC\",\"createdBy\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/iot/devices").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(deviceService.get(DEV)).thenReturn(deviceResp());
        mockMvc.perform(get("/api/v1/iot/devices/{id}", DEV))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_returns404() throws Exception {
        when(deviceService.get(DEV)).thenThrow(new IotDeviceNotFoundException(DEV));
        mockMvc.perform(get("/api/v1/iot/devices/{id}", DEV))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_patches() throws Exception {
        when(deviceService.update(any(), any())).thenReturn(deviceResp());
        mockMvc.perform(patch("/api/v1/iot/devices/{id}", DEV).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/iot/devices/{id}", DEV).with(csrf()))
                .andExpect(status().isNoContent());
        verify(deviceService).delete(DEV);
    }

    @Test @WithMockUser
    void activate_returns200() throws Exception {
        when(deviceService.activate(DEV)).thenReturn(deviceResp());
        mockMvc.perform(post("/api/v1/iot/devices/{id}/activate", DEV).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void suspend_returns200() throws Exception {
        when(deviceService.suspend(DEV)).thenReturn(deviceResp());
        mockMvc.perform(post("/api/v1/iot/devices/{id}/suspend", DEV).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void decommission_returns200() throws Exception {
        when(deviceService.decommission(DEV)).thenReturn(deviceResp());
        mockMvc.perform(post("/api/v1/iot/devices/{id}/decommission", DEV).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void invalidStateTransition_returns409() throws Exception {
        when(deviceService.activate(DEV))
                .thenThrow(new IotDeviceStateException("Cannot activate device in status DECOMMISSIONED"));
        mockMvc.perform(post("/api/v1/iot/devices/{id}/activate", DEV).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void ingest_returns201() throws Exception {
        when(telemetryService.ingest(eq(DEV), any())).thenReturn(telemetryResp());
        IotDto.TelemetryIngestRequest req = new IotDto.TelemetryIngestRequest(
                "temperature", new BigDecimal("21.5"), null, "C", null, null);
        mockMvc.perform(post("/api/v1/iot/devices/{id}/telemetry", DEV).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void ingest_blankMetric_returns400() throws Exception {
        String body = "{\"metric\":\"\",\"valueNumeric\":1}";
        mockMvc.perform(post("/api/v1/iot/devices/{id}/telemetry", DEV).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void recent_returns200() throws Exception {
        when(telemetryService.recent(eq(DEV), any()))
                .thenReturn(new PageImpl<>(List.of(telemetryResp())));
        mockMvc.perform(get("/api/v1/iot/devices/{id}/telemetry", DEV))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void range_returns200() throws Exception {
        when(telemetryService.range(eq(DEV), eq("temperature"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(telemetryResp())));
        mockMvc.perform(get("/api/v1/iot/devices/{id}/telemetry/range", DEV)
                        .param("metric", "temperature")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-01-02T00:00:00Z"))
                .andExpect(status().isOk());
    }

    private IotDto.DeviceResponse deviceResp() {
        return new IotDto.DeviceResponse(
                DEV, TENANT, "code-1", "Device 1",
                IotDeviceType.PLC, IotProtocol.OPC_UA, IotDeviceStatus.ACTIVE,
                null, null, null, null, 0, USER, Instant.now(), Instant.now());
    }

    private IotDto.TelemetryResponse telemetryResp() {
        return new IotDto.TelemetryResponse(
                UUID.randomUUID(), TENANT, DEV, "temperature",
                new BigDecimal("21.5"), null, "C", IotProtocol.MANUAL,
                Instant.now(), Instant.now());
    }
}
