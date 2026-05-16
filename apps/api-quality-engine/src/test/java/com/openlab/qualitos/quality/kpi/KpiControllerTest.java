package com.openlab.qualitos.quality.kpi;

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

@WebMvcTest(controllers = KpiController.class)
class KpiControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean KpiService service;
    ObjectMapper om;

    static final UUID KPI = UUID.randomUUID();
    static final UUID MEAS = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ----- Definition -----

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(kpiResp())));
        mockMvc.perform(get("/api/v1/kpis"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.create(any())).thenReturn(kpiResp());
        KpiDto.CreateKpiRequest req = new KpiDto.CreateKpiRequest(
                "first-pass-yield", "FPY", null, "quality", "%",
                KpiDirection.HIGHER_IS_BETTER, KpiFrequency.MONTHLY,
                new BigDecimal("95"), new BigDecimal("90"), new BigDecimal("80"),
                null, null, USER);
        mockMvc.perform(post("/api/v1/kpis").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_invalidCode_400() throws Exception {
        String body = "{\"code\":\"BAD-CODE\",\"name\":\"n\",\"direction\":\"HIGHER_IS_BETTER\","
                + "\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/kpis").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingDirection_400() throws Exception {
        String body = "{\"code\":\"k\",\"name\":\"n\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/kpis").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(KPI)).thenThrow(new KpiNotFoundException(KPI));
        mockMvc.perform(get("/api/v1/kpis/{id}", KPI))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_200() throws Exception {
        when(service.update(any(), any())).thenReturn(kpiResp());
        mockMvc.perform(patch("/api/v1/kpis/{id}", KPI).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/kpis/{id}", KPI).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).delete(KPI);
    }

    @Test @WithMockUser
    void activate_invalidState_409() throws Exception {
        when(service.activate(KPI)).thenThrow(new KpiStateException("archived"));
        mockMvc.perform(post("/api/v1/kpis/{id}/activate", KPI).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void activate_200() throws Exception {
        when(service.activate(KPI)).thenReturn(kpiResp());
        mockMvc.perform(post("/api/v1/kpis/{id}/activate", KPI).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void reopen_200() throws Exception {
        when(service.reopen(KPI)).thenReturn(kpiResp());
        mockMvc.perform(post("/api/v1/kpis/{id}/reopen", KPI).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void archive_200() throws Exception {
        when(service.archive(KPI)).thenReturn(kpiResp());
        mockMvc.perform(post("/api/v1/kpis/{id}/archive", KPI).with(csrf()))
                .andExpect(status().isOk());
    }

    // ----- Status / trend -----

    @Test @WithMockUser
    void status_200() throws Exception {
        when(service.currentStatus(KPI)).thenReturn(new KpiDto.KpiCurrentStatus(
                KPI, "fpy", "FPY", KpiStatus.ACTIVE, KpiDirection.HIGHER_IS_BETTER,
                new BigDecimal("92"), "%",
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-30T23:59:59Z"),
                KpiHealth.WARNING,
                new BigDecimal("95"), new BigDecimal("90"), new BigDecimal("80")));
        mockMvc.perform(get("/api/v1/kpis/{id}/status", KPI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.health").value("WARNING"));
    }

    @Test @WithMockUser
    void trend_200() throws Exception {
        when(service.trend(KPI)).thenReturn(new KpiDto.KpiTrend(
                KPI, "fpy", 0, List.of()));
        mockMvc.perform(get("/api/v1/kpis/{id}/trend", KPI))
                .andExpect(status().isOk());
    }

    // ----- Measurements -----

    @Test @WithMockUser
    void recordMeasurement_201() throws Exception {
        when(service.record(eq(KPI), any())).thenReturn(measurementResp());
        KpiDto.RecordMeasurementRequest req = new KpiDto.RecordMeasurementRequest(
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-30T23:59:59Z"),
                new BigDecimal("92"), "%", MeasurementSource.MANUAL, USER, null);
        mockMvc.perform(post("/api/v1/kpis/{id}/measurements", KPI).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void recordMeasurement_missingPeriod_400() throws Exception {
        String body = "{\"value\":92}";
        mockMvc.perform(post("/api/v1/kpis/{id}/measurements", KPI).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void recordMeasurement_invalidWindow_409() throws Exception {
        when(service.record(eq(KPI), any()))
                .thenThrow(new KpiStateException("periodStart before periodEnd"));
        KpiDto.RecordMeasurementRequest req = new KpiDto.RecordMeasurementRequest(
                Instant.parse("2026-04-30T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                new BigDecimal("92"), null, null, USER, null);
        mockMvc.perform(post("/api/v1/kpis/{id}/measurements", KPI).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void listMeasurements_200() throws Exception {
        when(service.listMeasurements(eq(KPI), any()))
                .thenReturn(new PageImpl<>(List.of(measurementResp())));
        mockMvc.perform(get("/api/v1/kpis/{id}/measurements", KPI))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void deleteMeasurement_204() throws Exception {
        mockMvc.perform(delete("/api/v1/kpis/{id}/measurements/{mid}", KPI, MEAS).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).deleteMeasurement(KPI, MEAS);
    }

    @Test @WithMockUser
    void deleteMeasurement_notFound_404() throws Exception {
        doThrow(new KpiMeasurementNotFoundException(MEAS))
                .when(service).deleteMeasurement(KPI, MEAS);
        mockMvc.perform(delete("/api/v1/kpis/{id}/measurements/{mid}", KPI, MEAS).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ----- factories -----

    private KpiDto.KpiResponse kpiResp() {
        return new KpiDto.KpiResponse(
                KPI, TENANT, "fpy", "FPY", null, "quality", "%",
                KpiDirection.HIGHER_IS_BETTER, KpiFrequency.MONTHLY,
                new BigDecimal("95"), new BigDecimal("90"), new BigDecimal("80"),
                KpiStatus.ACTIVE, null, null, USER,
                Instant.now(), Instant.now());
    }

    private KpiDto.MeasurementResponse measurementResp() {
        return new KpiDto.MeasurementResponse(
                MEAS, TENANT, KPI,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-04-30T23:59:59Z"),
                new BigDecimal("92"), "%",
                MeasurementSource.MANUAL, USER,
                null, KpiHealth.WARNING, Instant.now());
    }
}
