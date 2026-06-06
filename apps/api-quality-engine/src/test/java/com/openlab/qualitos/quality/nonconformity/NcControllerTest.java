package com.openlab.qualitos.quality.nonconformity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = NcController.class)
class NcControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean NcService service;
    ObjectMapper om;

    static final UUID NC = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID REPORTER = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.findAll(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resp(NcStatus.OPEN))));
        mockMvc.perform(get("/api/v1/nc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(NC.toString()));
    }

    @Test @WithMockUser
    void list_withFilters() throws Exception {
        when(service.findAll(eq(NcStatus.OPEN), eq(NcSeverity.MAJOR), eq(NcCategory.PRODUCT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resp(NcStatus.OPEN))));
        mockMvc.perform(get("/api/v1/nc")
                        .param("status", "OPEN").param("severity", "MAJOR").param("category", "PRODUCT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.create(any())).thenReturn(resp(NcStatus.OPEN));
        NcDto.CreateRequest req = new NcDto.CreateRequest(
                "t", null, NcCategory.PRODUCT, NcSeverity.MAJOR, Instant.now(),
                null, null, null, null, REPORTER);
        mockMvc.perform(post("/api/v1/nc").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("NC-2026-0001"));
    }

    @Test @WithMockUser
    void create_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/nc").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"PRODUCT\",\"severity\":\"MAJOR\",\"detectedAt\":\"2026-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingCategory_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/nc").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"severity\":\"MAJOR\",\"detectedAt\":\"2026-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingDetectedAt_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/nc").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"category\":\"PRODUCT\",\"severity\":\"MAJOR\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_latOutOfBounds_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/nc").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"category\":\"PRODUCT\",\"severity\":\"MAJOR\","
                                + "\"detectedAt\":\"2026-01-01T00:00:00Z\",\"geoLat\":95.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_lngOutOfBounds_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/nc").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"category\":\"PRODUCT\",\"severity\":\"MAJOR\","
                                + "\"detectedAt\":\"2026-01-01T00:00:00Z\",\"geoLng\":-200.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(service.findById(NC)).thenReturn(resp(NcStatus.OPEN));
        mockMvc.perform(get("/api/v1/nc/{id}", NC)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_returns404() throws Exception {
        when(service.findById(NC)).thenThrow(new NcNotFoundException(NC));
        mockMvc.perform(get("/api/v1/nc/{id}", NC)).andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_success() throws Exception {
        when(service.update(eq(NC), any())).thenReturn(resp(NcStatus.OPEN));
        mockMvc.perform(put("/api/v1/nc/{id}", NC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void update_closed_returns409() throws Exception {
        when(service.update(eq(NC), any())).thenThrow(new NcStateException("closed"));
        mockMvc.perform(put("/api/v1/nc/{id}", NC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void startAnalysis_success() throws Exception {
        when(service.startAnalysis(eq(NC), any())).thenReturn(resp(NcStatus.UNDER_ANALYSIS));
        mockMvc.perform(post("/api/v1/nc/{id}/start-analysis", NC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rootCause\":\"c\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_ANALYSIS"));
    }

    @Test @WithMockUser
    void startAnalysis_noBody_ok() throws Exception {
        when(service.startAnalysis(eq(NC), any())).thenReturn(resp(NcStatus.UNDER_ANALYSIS));
        mockMvc.perform(post("/api/v1/nc/{id}/start-analysis", NC).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void startAnalysis_invalid_returns409() throws Exception {
        when(service.startAnalysis(eq(NC), any())).thenThrow(new NcStateException("nope"));
        mockMvc.perform(post("/api/v1/nc/{id}/start-analysis", NC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void defineAction_success() throws Exception {
        when(service.defineAction(NC)).thenReturn(resp(NcStatus.ACTION_DEFINED));
        mockMvc.perform(post("/api/v1/nc/{id}/define-action", NC).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTION_DEFINED"));
    }

    @Test @WithMockUser
    void resolve_success() throws Exception {
        when(service.resolve(eq(NC), any())).thenReturn(resp(NcStatus.RESOLVED));
        mockMvc.perform(post("/api/v1/nc/{id}/resolve", NC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolutionNote\":\"corrigé\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test @WithMockUser
    void resolve_missingNote_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/nc/{id}/resolve", NC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void close_success() throws Exception {
        when(service.close(NC)).thenReturn(resp(NcStatus.CLOSED));
        mockMvc.perform(post("/api/v1/nc/{id}/close", NC).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test @WithMockUser
    void cancel_success() throws Exception {
        when(service.cancel(NC)).thenReturn(resp(NcStatus.CANCELLED));
        mockMvc.perform(post("/api/v1/nc/{id}/cancel", NC).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test @WithMockUser
    void escalate_success() throws Exception {
        when(service.escalateToCapa(eq(NC), any())).thenReturn(resp(NcStatus.UNDER_ANALYSIS));
        mockMvc.perform(post("/api/v1/nc/{id}/escalate-capa", NC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerId\":\"" + OWNER + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void escalate_missingOwner_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/nc/{id}/escalate-capa", NC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void escalate_alreadyDone_returns409() throws Exception {
        when(service.escalateToCapa(eq(NC), any())).thenThrow(new NcStateException("already escalated"));
        mockMvc.perform(post("/api/v1/nc/{id}/escalate-capa", NC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerId\":\"" + OWNER + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void create_missingTenant_returns403() throws Exception {
        when(service.create(any())).thenThrow(new MissingTenantContextException());
        NcDto.CreateRequest req = new NcDto.CreateRequest(
                "t", null, NcCategory.PRODUCT, NcSeverity.MAJOR, Instant.now(),
                null, null, null, null, REPORTER);
        mockMvc.perform(post("/api/v1/nc").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // helper
    private NcDto.Response resp(NcStatus s) {
        return new NcDto.Response(
                NC, TENANT, "NC-2026-0001", "t", null,
                NcCategory.PRODUCT, NcSeverity.MAJOR, s, Instant.now(),
                null, null, null, null, REPORTER, null, null, null,
                null, null, Instant.now(), Instant.now());
    }
}
