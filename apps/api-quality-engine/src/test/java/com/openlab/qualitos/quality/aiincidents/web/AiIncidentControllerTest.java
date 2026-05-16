package com.openlab.qualitos.quality.aiincidents.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.aiincidents.application.AiIncidentDto;
import com.openlab.qualitos.quality.aiincidents.application.AiIncidentService;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentNotFoundException;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentSeverity;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStateException;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = AiIncidentController.class)
class AiIncidentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AiIncidentService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID LEAD = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant OCCURRED = NOW.minusSeconds(3600);

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(null)).thenReturn(List.of(view(AiIncidentStatus.DETECTED)));
        mockMvc.perform(get("/api/v1/ai-act/incidents")).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_byStatus_200() throws Exception {
        when(service.list(AiIncidentStatus.CLOSED))
                .thenReturn(List.of(view(AiIncidentStatus.CLOSED)));
        mockMvc.perform(get("/api/v1/ai-act/incidents").param("status", "CLOSED"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_invalidStatus_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/incidents").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void bySystem_200() throws Exception {
        when(service.listByAiSystem(SYS)).thenReturn(List.of(view(AiIncidentStatus.DETECTED)));
        mockMvc.perform(get("/api/v1/ai-act/incidents/by-system")
                        .param("aiSystemId", SYS.toString()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void bySeverity_200() throws Exception {
        when(service.listBySeverity(AiIncidentSeverity.DEATH_OR_SERIOUS_HARM_TO_HEALTH))
                .thenReturn(List.of(view(AiIncidentStatus.DETECTED)));
        mockMvc.perform(get("/api/v1/ai-act/incidents/by-severity")
                        .param("severity", "DEATH_OR_SERIOUS_HARM_TO_HEALTH"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void bySeverity_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/incidents/by-severity").param("severity", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void overdue_200() throws Exception {
        when(service.listOverdueForRegulator(200))
                .thenReturn(List.of(view(AiIncidentStatus.DETECTED)));
        mockMvc.perform(get("/api/v1/ai-act/incidents/overdue-regulator-notification"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void overdue_outOfRange_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/incidents/overdue-regulator-notification")
                        .param("limit", "0"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void get_200() throws Exception {
        when(service.get(ID)).thenReturn(view(AiIncidentStatus.DETECTED));
        mockMvc.perform(get("/api/v1/ai-act/incidents/{id}", ID)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new AiIncidentNotFoundException(ID));
        mockMvc.perform(get("/api/v1/ai-act/incidents/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("REF-1")).thenReturn(view(AiIncidentStatus.DETECTED));
        mockMvc.perform(get("/api/v1/ai-act/incidents/by-reference").param("reference", "REF-1"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/incidents/by-reference")
                        .param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void detect_201() throws Exception {
        when(service.detect(any())).thenReturn(view(AiIncidentStatus.DETECTED));
        AiIncidentWebDto.DetectRequest req = new AiIncidentWebDto.DetectRequest(
                "REF-1", SYS, AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "desc", null, null, OCCURRED, NOW, USER);
        mockMvc.perform(post("/api/v1/ai-act/incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void detect_invalidRef_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"aiSystemId\":\"" + SYS
                + "\",\"severity\":\"CRITICAL_INFRASTRUCTURE_DISRUPTION\","
                + "\"description\":\"d\",\"occurredAt\":\"2026-05-16T08:00:00Z\","
                + "\"detectedAt\":\"2026-05-16T10:00:00Z\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void detect_missingSeverity_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"aiSystemId\":\"" + SYS + "\","
                + "\"description\":\"d\",\"occurredAt\":\"2026-05-16T08:00:00Z\","
                + "\"detectedAt\":\"2026-05-16T10:00:00Z\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void detect_duplicate_409() throws Exception {
        when(service.detect(any()))
                .thenThrow(new AiIncidentStateException("Reference already used"));
        AiIncidentWebDto.DetectRequest req = new AiIncidentWebDto.DetectRequest(
                "REF-DUP", SYS, AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "desc", null, null, OCCURRED, NOW, USER);
        mockMvc.perform(post("/api/v1/ai-act/incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(AiIncidentStatus.DETECTED));
        AiIncidentWebDto.EditRequest req = new AiIncidentWebDto.EditRequest("new", null, null);
        mockMvc.perform(put("/api/v1/ai-act/incidents/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void startInvestigation_200() throws Exception {
        when(service.startInvestigation(eq(ID), any()))
                .thenReturn(view(AiIncidentStatus.INVESTIGATING));
        mockMvc.perform(post("/api/v1/ai-act/incidents/{id}/start-investigation", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"investigationLeadUserId\":\"" + LEAD + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void notifyRegulator_200() throws Exception {
        when(service.notifyRegulator(eq(ID), any()))
                .thenReturn(view(AiIncidentStatus.NOTIFIED_REGULATOR));
        mockMvc.perform(post("/api/v1/ai-act/incidents/{id}/notify-regulator", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regulatorReference\":\"REG-1\",\"rootCauseAnalysis\":\"rca\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void notifyRegulator_missingRef_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/incidents/{id}/notify-regulator", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rootCauseAnalysis\":\"rca\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void close_200() throws Exception {
        when(service.close(eq(ID), any())).thenReturn(view(AiIncidentStatus.CLOSED));
        mockMvc.perform(post("/api/v1/ai-act/incidents/{id}/close", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correctiveActions\":\"done\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void close_blankActions_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/incidents/{id}/close", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correctiveActions\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void dismiss_200() throws Exception {
        when(service.dismiss(eq(ID), any())).thenReturn(view(AiIncidentStatus.DISMISSED));
        mockMvc.perform(post("/api/v1/ai-act/incidents/{id}/dismiss", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"false alarm\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/ai-act/incidents/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private AiIncidentDto.View view(AiIncidentStatus status) {
        Instant due = NOW.plusSeconds(15L * 86400);
        return new AiIncidentDto.View(
                ID, TENANT, "REF-1", SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "desc", "persons", "actions", OCCURRED, NOW, status,
                status == AiIncidentStatus.INVESTIGATING || status == AiIncidentStatus.NOTIFIED_REGULATOR
                        || status == AiIncidentStatus.CLOSED ? NOW : null,
                status == AiIncidentStatus.INVESTIGATING || status == AiIncidentStatus.NOTIFIED_REGULATOR
                        || status == AiIncidentStatus.CLOSED ? LEAD : null,
                status == AiIncidentStatus.NOTIFIED_REGULATOR || status == AiIncidentStatus.CLOSED
                        ? "rca" : null,
                status == AiIncidentStatus.CLOSED ? "actions" : null,
                status == AiIncidentStatus.NOTIFIED_REGULATOR || status == AiIncidentStatus.CLOSED
                        ? NOW : null,
                status == AiIncidentStatus.NOTIFIED_REGULATOR || status == AiIncidentStatus.CLOSED
                        ? "REG-1" : null,
                status == AiIncidentStatus.CLOSED ? NOW : null,
                status == AiIncidentStatus.DISMISSED ? NOW : null,
                status == AiIncidentStatus.DISMISSED ? "false alarm" : null,
                due, USER, NOW, NOW);
    }
}
