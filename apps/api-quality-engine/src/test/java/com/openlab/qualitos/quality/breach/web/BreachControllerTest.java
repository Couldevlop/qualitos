package com.openlab.qualitos.quality.breach.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.breach.application.BreachDto;
import com.openlab.qualitos.quality.breach.application.BreachService;
import com.openlab.qualitos.quality.breach.domain.BreachNotFoundException;
import com.openlab.qualitos.quality.breach.domain.BreachSeverity;
import com.openlab.qualitos.quality.breach.domain.BreachStateException;
import com.openlab.qualitos.quality.breach.domain.BreachStatus;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BreachController.class)
class BreachControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean BreachService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID HANDLER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(null)).thenReturn(List.of(view(BreachStatus.DETECTED, BreachSeverity.LOW)));
        mockMvc.perform(get("/api/v1/gdpr/breaches"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_byStatus_200() throws Exception {
        when(service.list(BreachStatus.CONTAINED))
                .thenReturn(List.of(view(BreachStatus.CONTAINED, BreachSeverity.MEDIUM)));
        mockMvc.perform(get("/api/v1/gdpr/breaches").param("status", "CONTAINED"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new BreachNotFoundException(ID));
        mockMvc.perform(get("/api/v1/gdpr/breaches/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void detect_201() throws Exception {
        when(service.detect(any())).thenReturn(view(BreachStatus.DETECTED, BreachSeverity.HIGH));
        BreachWebDto.DetectRequest req = new BreachWebDto.DetectRequest(
                "BREACH-2026-001", "Lost laptop", null,
                NOW, null, BreachSeverity.HIGH, 1500L,
                Set.of("customer-pii"), "risk", USER);
        mockMvc.perform(post("/api/v1/gdpr/breaches").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjectNotificationRequired").value(true));
    }

    @Test @WithMockUser
    void detect_invalidReferenceLowercase_400() throws Exception {
        String body = "{\"internalReference\":\"breach-bad\",\"title\":\"t\",\"detectedAt\":\""
                + NOW + "\",\"severity\":\"LOW\",\"affectedSubjectsCount\":0,\"reportedByUserId\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/breaches").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void detect_negativeCount_400() throws Exception {
        String body = "{\"internalReference\":\"BREACH-1\",\"title\":\"t\",\"detectedAt\":\""
                + NOW + "\",\"severity\":\"LOW\",\"affectedSubjectsCount\":-1,\"reportedByUserId\":\""
                + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/breaches").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void detect_duplicateRef_409() throws Exception {
        when(service.detect(any()))
                .thenThrow(new BreachStateException("Reference already used"));
        BreachWebDto.DetectRequest req = new BreachWebDto.DetectRequest(
                "BREACH-1", "t", null, NOW, null, BreachSeverity.LOW, 0L,
                Set.of(), null, USER);
        mockMvc.perform(post("/api/v1/gdpr/breaches").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void startAssessment_200() throws Exception {
        when(service.startAssessment(eq(ID), any()))
                .thenReturn(view(BreachStatus.ASSESSING, BreachSeverity.LOW));
        mockMvc.perform(post("/api/v1/gdpr/breaches/{id}/start-assessment", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handledByUserId\":\"" + HANDLER + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void contain_200() throws Exception {
        when(service.contain(eq(ID), any()))
                .thenReturn(view(BreachStatus.CONTAINED, BreachSeverity.LOW));
        mockMvc.perform(post("/api/v1/gdpr/breaches/{id}/contain", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"containmentMeasures\":\"steps\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void contain_blankMeasures_400() throws Exception {
        mockMvc.perform(post("/api/v1/gdpr/breaches/{id}/contain", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"containmentMeasures\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void notifyDpa_200() throws Exception {
        when(service.notifyDpa(eq(ID), any()))
                .thenReturn(view(BreachStatus.CONTAINED, BreachSeverity.MEDIUM));
        mockMvc.perform(post("/api/v1/gdpr/breaches/{id}/notify-dpa", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifiedAt\":\"" + NOW + "\",\"reference\":\"CNIL-1\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void notifySubjects_200() throws Exception {
        when(service.notifySubjects(eq(ID), any()))
                .thenReturn(view(BreachStatus.CONTAINED, BreachSeverity.HIGH));
        mockMvc.perform(post("/api/v1/gdpr/breaches/{id}/notify-subjects", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifiedAt\":\"" + NOW + "\",\"channel\":\"email\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void close_200() throws Exception {
        when(service.close(eq(ID), any()))
                .thenReturn(view(BreachStatus.CLOSED, BreachSeverity.LOW));
        mockMvc.perform(post("/api/v1/gdpr/breaches/{id}/close", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closureNotes\":\"done\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void close_invalidState_409() throws Exception {
        when(service.close(eq(ID), any()))
                .thenThrow(new BreachStateException("subjects notification required"));
        mockMvc.perform(post("/api/v1/gdpr/breaches/{id}/close", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void reject_200() throws Exception {
        when(service.reject(eq(ID), any()))
                .thenReturn(view(BreachStatus.REJECTED, BreachSeverity.LOW));
        mockMvc.perform(post("/api/v1/gdpr/breaches/{id}/reject", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"false positive\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void updateSeverity_200() throws Exception {
        when(service.updateSeverity(eq(ID), any()))
                .thenReturn(view(BreachStatus.DETECTED, BreachSeverity.CRITICAL));
        mockMvc.perform(post("/api/v1/gdpr/breaches/{id}/severity", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"CRITICAL\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void dpaOverdue_200() throws Exception {
        when(service.dpaOverdue(100))
                .thenReturn(List.of(view(BreachStatus.DETECTED, BreachSeverity.HIGH)));
        mockMvc.perform(get("/api/v1/gdpr/breaches/dpa-overdue"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void dpaOverdue_outOfRange_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/breaches/dpa-overdue").param("limit", "0"))
                .andExpect(status().is4xxClientError());
    }

    private BreachDto.View view(BreachStatus status, BreachSeverity sev) {
        return new BreachDto.View(
                ID, TENANT, "BREACH-2026-001", "Lost laptop", null,
                NOW, null, NOW.plus(Duration.ofHours(72)),
                sev, status, 1500L, Set.of("customer-pii"),
                "risk", status == BreachStatus.CONTAINED ? "rotated keys" : null,
                null, null, null, null,
                status == BreachStatus.REJECTED ? "false positive" : null, null,
                USER, HANDLER, status == BreachStatus.CLOSED ? NOW : null, NOW,
                false, sev.requiresSubjectNotification());
    }
}
