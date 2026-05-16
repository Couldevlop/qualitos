package com.openlab.qualitos.quality.retention.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.retention.application.RetentionRuleDto;
import com.openlab.qualitos.quality.retention.application.RetentionRuleService;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleNotFoundException;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleStateException;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = RetentionRuleController.class)
class RetentionRuleControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RetentionRuleService service;
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
        when(service.list(null)).thenReturn(List.of(view(RetentionRuleStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/gdpr/retention-rules"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new RetentionRuleNotFoundException(ID));
        mockMvc.perform(get("/api/v1/gdpr/retention-rules/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.create(any())).thenReturn(view(RetentionRuleStatus.DRAFT));
        RetentionRuleWebDto.CreateRequest req = new RetentionRuleWebDto.CreateRequest(
                "marketing", "Marketing data", Duration.ofDays(30),
                "Consent (Art. 6.1.a)", null, USER);
        mockMvc.perform(post("/api/v1/gdpr/retention-rules").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_invalidCategoryCode_400() throws Exception {
        String body = "{\"dataCategoryCode\":\"BAD CODE\",\"retentionPeriod\":\"P30D\","
                + "\"legalBasis\":\"basis\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/retention-rules").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingPeriod_400() throws Exception {
        String body = "{\"dataCategoryCode\":\"marketing\","
                + "\"legalBasis\":\"basis\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/retention-rules").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_blankLegalBasis_400() throws Exception {
        String body = "{\"dataCategoryCode\":\"marketing\",\"retentionPeriod\":\"P30D\","
                + "\"legalBasis\":\"\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/retention-rules").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(RetentionRuleStatus.DRAFT));
        RetentionRuleWebDto.EditRequest req = new RetentionRuleWebDto.EditRequest(
                "Updated", Duration.ofDays(60), "new basis", null);
        mockMvc.perform(put("/api/v1/gdpr/retention-rules/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void edit_onActive_409() throws Exception {
        when(service.edit(eq(ID), any()))
                .thenThrow(new RetentionRuleStateException("Only DRAFT rules can be edited"));
        RetentionRuleWebDto.EditRequest req = new RetentionRuleWebDto.EditRequest(
                null, Duration.ofDays(60), "basis", null);
        mockMvc.perform(put("/api/v1/gdpr/retention-rules/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void activate_200() throws Exception {
        when(service.activate(ID)).thenReturn(view(RetentionRuleStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/gdpr/retention-rules/{id}/activate", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void archive_200() throws Exception {
        when(service.archive(ID)).thenReturn(view(RetentionRuleStatus.ARCHIVED));
        mockMvc.perform(post("/api/v1/gdpr/retention-rules/{id}/archive", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/gdpr/retention-rules/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void delete_onActive_409() throws Exception {
        org.mockito.Mockito.doThrow(new RetentionRuleStateException("active preserved for audit"))
                .when(service).delete(ID);
        mockMvc.perform(delete("/api/v1/gdpr/retention-rules/{id}", ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void evaluate_found_200() throws Exception {
        when(service.evaluateErasure("marketing", NOW))
                .thenReturn(Optional.of(new RetentionRuleDto.ErasureEvaluation(
                        "marketing", NOW, NOW.plusSeconds(86400L * 30), false,
                        ID, Duration.ofDays(30))));
        mockMvc.perform(get("/api/v1/gdpr/retention-rules/erasure-evaluation")
                        .param("dataCategoryCode", "marketing")
                        .param("recordCreatedAt", NOW.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueNow").value(false));
    }

    @Test @WithMockUser
    void evaluate_noRule_404() throws Exception {
        when(service.evaluateErasure("marketing", NOW)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/gdpr/retention-rules/erasure-evaluation")
                        .param("dataCategoryCode", "marketing")
                        .param("recordCreatedAt", NOW.toString()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void evaluate_invalidCategory_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/retention-rules/erasure-evaluation")
                        .param("dataCategoryCode", "BAD CODE")
                        .param("recordCreatedAt", NOW.toString()))
                .andExpect(status().is4xxClientError());
    }

    private RetentionRuleDto.View view(RetentionRuleStatus status) {
        return new RetentionRuleDto.View(
                ID, TENANT, "marketing", "Marketing data",
                Duration.ofDays(30), "basis", null,
                status,
                status == RetentionRuleStatus.ACTIVE ? NOW : null,
                status == RetentionRuleStatus.ARCHIVED ? NOW.plusSeconds(60) : null,
                USER, NOW, NOW);
    }
}
