package com.openlab.qualitos.quality.automateddecisions.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.automateddecisions.application.AutomatedDecisionDto;
import com.openlab.qualitos.quality.automateddecisions.application.AutomatedDecisionService;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionNotFoundException;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStateException;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStatus;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AutomatedDecisionController.class)
class AutomatedDecisionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AutomatedDecisionService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(null)).thenReturn(List.of(view(AutomatedDecisionStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/gdpr/automated-decisions"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new AutomatedDecisionNotFoundException(ID));
        mockMvc.perform(get("/api/v1/gdpr/automated-decisions/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("ADM-2026-001"))
                .thenReturn(view(AutomatedDecisionStatus.ACTIVE));
        mockMvc.perform(get("/api/v1/gdpr/automated-decisions/by-reference")
                        .param("reference", "ADM-2026-001"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/automated-decisions/by-reference")
                        .param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.create(any())).thenReturn(view(AutomatedDecisionStatus.DRAFT));
        AutomatedDecisionWebDto.CreateRequest req = new AutomatedDecisionWebDto.CreateRequest(
                "ADM-2026-001", "Recommandations", null,
                AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null,
                null, null, null, null, USER);
        mockMvc.perform(post("/api/v1/gdpr/automated-decisions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_invalidReference_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"name\":\"N\","
                + "\"decisionType\":\"PROFILING_ONLY\",\"createdByUserId\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/automated-decisions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_blankName_400() throws Exception {
        String body = "{\"reference\":\"ADM-1\",\"name\":\"\","
                + "\"decisionType\":\"PROFILING_ONLY\",\"createdByUserId\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/automated-decisions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingDecisionType_400() throws Exception {
        String body = "{\"reference\":\"ADM-1\",\"name\":\"N\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/automated-decisions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingActor_400() throws Exception {
        String body = "{\"reference\":\"ADM-1\",\"name\":\"N\","
                + "\"decisionType\":\"PROFILING_ONLY\"}";
        mockMvc.perform(post("/api/v1/gdpr/automated-decisions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_invalidDecisionType_400() throws Exception {
        String body = "{\"reference\":\"ADM-1\",\"name\":\"N\","
                + "\"decisionType\":\"WHATEVER\",\"createdByUserId\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/automated-decisions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_duplicate_409() throws Exception {
        when(service.create(any()))
                .thenThrow(new AutomatedDecisionStateException("Reference already used"));
        AutomatedDecisionWebDto.CreateRequest req = new AutomatedDecisionWebDto.CreateRequest(
                "ADM-DUP", "N", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null,
                null, null, null, null, USER);
        mockMvc.perform(post("/api/v1/gdpr/automated-decisions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(AutomatedDecisionStatus.DRAFT));
        AutomatedDecisionWebDto.EditRequest req = new AutomatedDecisionWebDto.EditRequest(
                "Updated", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null,
                null, null, null, null);
        mockMvc.perform(put("/api/v1/gdpr/automated-decisions/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void activate_200() throws Exception {
        when(service.activate(ID)).thenReturn(view(AutomatedDecisionStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/gdpr/automated-decisions/{id}/activate", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void activate_invalidState_409() throws Exception {
        when(service.activate(ID))
                .thenThrow(new AutomatedDecisionStateException("Art. 22.3 required"));
        mockMvc.perform(post("/api/v1/gdpr/automated-decisions/{id}/activate", ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void deprecate_200() throws Exception {
        when(service.deprecate(ID)).thenReturn(view(AutomatedDecisionStatus.DEPRECATED));
        mockMvc.perform(post("/api/v1/gdpr/automated-decisions/{id}/deprecate", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void archive_200() throws Exception {
        when(service.archive(ID)).thenReturn(view(AutomatedDecisionStatus.ARCHIVED));
        mockMvc.perform(post("/api/v1/gdpr/automated-decisions/{id}/archive", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/gdpr/automated-decisions/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private AutomatedDecisionDto.View view(AutomatedDecisionStatus status) {
        return new AutomatedDecisionDto.View(
                ID, TENANT, "ADM-2026-001", "Recommandations", null,
                AutomatedDecisionType.PROFILING_ONLY, null, null,
                Set.of(), Set.of(), null,
                null, null, null, null,
                status,
                status == AutomatedDecisionStatus.ACTIVE
                        || status == AutomatedDecisionStatus.DEPRECATED ? NOW : null,
                status == AutomatedDecisionStatus.ARCHIVED ? NOW.plusSeconds(60) : null,
                USER, NOW, NOW);
    }
}
