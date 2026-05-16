package com.openlab.qualitos.quality.cyberincidents.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.cyberincidents.application.CyberIncidentDto;
import com.openlab.qualitos.quality.cyberincidents.application.CyberIncidentService;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentNotFoundException;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentSeverity;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStateException;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStatus;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentType;
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

@WebMvcTest(controllers = CyberIncidentController.class)
class CyberIncidentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CyberIncidentService service;
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
        when(service.list(null)).thenReturn(List.of(view(CyberIncidentStatus.DETECTED)));
        mockMvc.perform(get("/api/v1/nis2/cyber-incidents"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new CyberIncidentNotFoundException(ID));
        mockMvc.perform(get("/api/v1/nis2/cyber-incidents/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("CYB-2026-001"))
                .thenReturn(view(CyberIncidentStatus.DETECTED));
        mockMvc.perform(get("/api/v1/nis2/cyber-incidents/by-reference")
                        .param("reference", "CYB-2026-001"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/nis2/cyber-incidents/by-reference")
                        .param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void earlyWarningOverdue_200() throws Exception {
        when(service.earlyWarningOverdue(100))
                .thenReturn(List.of(view(CyberIncidentStatus.DETECTED)));
        mockMvc.perform(get("/api/v1/nis2/cyber-incidents/early-warning-overdue"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void initialAssessmentOverdue_200() throws Exception {
        when(service.initialAssessmentOverdue(100))
                .thenReturn(List.of(view(CyberIncidentStatus.ASSESSING)));
        mockMvc.perform(get("/api/v1/nis2/cyber-incidents/initial-assessment-overdue"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void finalReportOverdue_200() throws Exception {
        when(service.finalReportOverdue(100))
                .thenReturn(List.of(view(CyberIncidentStatus.MITIGATED)));
        mockMvc.perform(get("/api/v1/nis2/cyber-incidents/final-report-overdue"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void overdue_outOfRange_400() throws Exception {
        mockMvc.perform(get("/api/v1/nis2/cyber-incidents/early-warning-overdue")
                        .param("limit", "0"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void detect_201() throws Exception {
        when(service.detect(any())).thenReturn(view(CyberIncidentStatus.DETECTED));
        CyberIncidentWebDto.DetectRequest req = new CyberIncidentWebDto.DetectRequest(
                "CYB-2026-001", "Ransomware", null,
                NOW, null, CyberIncidentType.RANSOMWARE, CyberIncidentSeverity.HIGH,
                500L, Set.of(), Set.of(), null, USER);
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.significant").value(true));
    }

    @Test @WithMockUser
    void detect_invalidRef_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"title\":\"t\",\"detectedAt\":\""
                + NOW + "\",\"incidentType\":\"MALWARE\",\"severity\":\"LOW\","
                + "\"estimatedAffectedUsers\":0,\"reportedByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void detect_negativeUsers_400() throws Exception {
        String body = "{\"reference\":\"CYB-1\",\"title\":\"t\",\"detectedAt\":\""
                + NOW + "\",\"incidentType\":\"MALWARE\",\"severity\":\"LOW\","
                + "\"estimatedAffectedUsers\":-1,\"reportedByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void detect_invalidType_400() throws Exception {
        String body = "{\"reference\":\"CYB-1\",\"title\":\"t\",\"detectedAt\":\""
                + NOW + "\",\"incidentType\":\"WHATEVER\",\"severity\":\"LOW\","
                + "\"estimatedAffectedUsers\":0,\"reportedByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void detect_duplicate_409() throws Exception {
        when(service.detect(any()))
                .thenThrow(new CyberIncidentStateException("Reference already used"));
        CyberIncidentWebDto.DetectRequest req = new CyberIncidentWebDto.DetectRequest(
                "CYB-1", "t", null, NOW, null,
                CyberIncidentType.MALWARE, CyberIncidentSeverity.LOW, 0L,
                Set.of(), Set.of(), null, USER);
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void startAssessment_200() throws Exception {
        when(service.startAssessment(eq(ID), any()))
                .thenReturn(view(CyberIncidentStatus.ASSESSING));
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents/{id}/start-assessment", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handledByUserId\":\"" + HANDLER + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void mitigate_200() throws Exception {
        when(service.mitigate(eq(ID), any()))
                .thenReturn(view(CyberIncidentStatus.MITIGATED));
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents/{id}/mitigate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"containmentMeasures\":\"patched\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void mitigate_blankMeasures_400() throws Exception {
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents/{id}/mitigate", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"containmentMeasures\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void earlyWarning_200() throws Exception {
        when(service.recordEarlyWarning(eq(ID), any()))
                .thenReturn(view(CyberIncidentStatus.ASSESSING));
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents/{id}/early-warning", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentAt\":\"" + NOW + "\",\"reference\":\"CSIRT-1\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void initialAssessment_200() throws Exception {
        when(service.recordInitialAssessment(eq(ID), any()))
                .thenReturn(view(CyberIncidentStatus.ASSESSING));
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents/{id}/initial-assessment", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentAt\":\"" + NOW + "\",\"reference\":\"IA-1\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void finalReport_200() throws Exception {
        when(service.recordFinalReport(eq(ID), any()))
                .thenReturn(view(CyberIncidentStatus.MITIGATED));
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents/{id}/final-report", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentAt\":\"" + NOW + "\",\"reference\":\"FINAL-1\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void close_200() throws Exception {
        when(service.close(eq(ID), any())).thenReturn(view(CyberIncidentStatus.CLOSED));
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents/{id}/close", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void close_invalidState_409() throws Exception {
        when(service.close(eq(ID), any()))
                .thenThrow(new CyberIncidentStateException("NIS2 Art. 23.4.c required"));
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents/{id}/close", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void reject_200() throws Exception {
        when(service.reject(eq(ID), any())).thenReturn(view(CyberIncidentStatus.REJECTED));
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents/{id}/reject", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"false positive\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void updateSeverity_200() throws Exception {
        when(service.updateSeverity(eq(ID), any()))
                .thenReturn(view(CyberIncidentStatus.ASSESSING));
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents/{id}/severity", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"CRITICAL\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void linkBreach_200() throws Exception {
        when(service.linkBreach(eq(ID), any()))
                .thenReturn(view(CyberIncidentStatus.ASSESSING));
        mockMvc.perform(post("/api/v1/nis2/cyber-incidents/{id}/link-breach", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"breachId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());
    }

    private CyberIncidentDto.View view(CyberIncidentStatus status) {
        return new CyberIncidentDto.View(
                ID, TENANT, "CYB-2026-001", "Ransomware", null,
                NOW, null,
                NOW.plus(Duration.ofHours(24)),
                NOW.plus(Duration.ofHours(72)),
                NOW.plus(Duration.ofDays(30)),
                CyberIncidentType.RANSOMWARE, CyberIncidentSeverity.HIGH,
                status, 500L, Set.of(), Set.of(), null,
                status == CyberIncidentStatus.MITIGATED ? "patched" : null, null,
                null, null, null, null, null, null,
                null, status == CyberIncidentStatus.REJECTED ? "FP" : null,
                USER, HANDLER,
                status == CyberIncidentStatus.CLOSED || status == CyberIncidentStatus.REJECTED ? NOW : null,
                NOW, false, false, false, true);
    }
}
