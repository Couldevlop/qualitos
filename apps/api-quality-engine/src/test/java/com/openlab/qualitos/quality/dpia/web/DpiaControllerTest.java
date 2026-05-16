package com.openlab.qualitos.quality.dpia.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.dpia.application.DpiaDto;
import com.openlab.qualitos.quality.dpia.application.DpiaService;
import com.openlab.qualitos.quality.dpia.domain.DpiaNotFoundException;
import com.openlab.qualitos.quality.dpia.domain.DpiaStateException;
import com.openlab.qualitos.quality.dpia.domain.DpiaStatus;
import com.openlab.qualitos.quality.dpia.domain.RiskLevel;
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

@WebMvcTest(controllers = DpiaController.class)
class DpiaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DpiaService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID DPO = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(null)).thenReturn(List.of(view(DpiaStatus.DRAFT, RiskLevel.LOW)));
        mockMvc.perform(get("/api/v1/gdpr/dpias"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_byStatus_200() throws Exception {
        when(service.list(DpiaStatus.DPO_REVIEW))
                .thenReturn(List.of(view(DpiaStatus.DPO_REVIEW, RiskLevel.MEDIUM)));
        mockMvc.perform(get("/api/v1/gdpr/dpias").param("status", "DPO_REVIEW"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void requiringConsultation_200() throws Exception {
        when(service.requiringConsultation())
                .thenReturn(List.of(view(DpiaStatus.DPO_REVIEW, RiskLevel.HIGH)));
        mockMvc.perform(get("/api/v1/gdpr/dpias/requiring-consultation"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new DpiaNotFoundException(ID));
        mockMvc.perform(get("/api/v1/gdpr/dpias/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("DPIA-2026-001"))
                .thenReturn(view(DpiaStatus.APPROVED, RiskLevel.LOW));
        mockMvc.perform(get("/api/v1/gdpr/dpias/by-reference")
                        .param("reference", "DPIA-2026-001"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/gdpr/dpias/by-reference")
                        .param("reference", "lowercase-bad"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.create(any())).thenReturn(view(DpiaStatus.DRAFT, RiskLevel.LOW));
        DpiaWebDto.CreateRequest req = new DpiaWebDto.CreateRequest(
                "DPIA-2026-001", "Hiring check", null,
                Set.of(UUID.randomUUID()), RiskLevel.LOW, USER);
        mockMvc.perform(post("/api/v1/gdpr/dpias").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_lowercaseReference_400() throws Exception {
        String body = "{\"reference\":\"dpia-bad\",\"title\":\"t\","
                + "\"initialRiskLevel\":\"LOW\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/dpias").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingInitialRisk_400() throws Exception {
        String body = "{\"reference\":\"DPIA-1\",\"title\":\"t\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/gdpr/dpias").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_duplicate_409() throws Exception {
        when(service.create(any()))
                .thenThrow(new DpiaStateException("Reference already used"));
        DpiaWebDto.CreateRequest req = new DpiaWebDto.CreateRequest(
                "DPIA-DUP", "t", null, Set.of(), RiskLevel.LOW, USER);
        mockMvc.perform(post("/api/v1/gdpr/dpias").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(DpiaStatus.DRAFT, RiskLevel.MEDIUM));
        DpiaWebDto.EditRequest req = new DpiaWebDto.EditRequest(
                "Updated", null, Set.of(), "n", "r", "m",
                RiskLevel.MEDIUM, false, null);
        mockMvc.perform(put("/api/v1/gdpr/dpias/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void start_200() throws Exception {
        when(service.start(eq(ID), any()))
                .thenReturn(view(DpiaStatus.IN_PROGRESS, RiskLevel.LOW));
        mockMvc.perform(post("/api/v1/gdpr/dpias/{id}/start", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handledByUserId\":\"" + USER + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void returnToDraft_200() throws Exception {
        when(service.returnToDraft(ID)).thenReturn(view(DpiaStatus.DRAFT, RiskLevel.LOW));
        mockMvc.perform(post("/api/v1/gdpr/dpias/{id}/return-to-draft", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void submitToDpo_200() throws Exception {
        when(service.submitToDpo(ID))
                .thenReturn(view(DpiaStatus.DPO_REVIEW, RiskLevel.LOW));
        mockMvc.perform(post("/api/v1/gdpr/dpias/{id}/submit-to-dpo", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void submitToDpo_missingFields_409() throws Exception {
        when(service.submitToDpo(ID))
                .thenThrow(new DpiaStateException("necessityAndProportionalityNotes required"));
        mockMvc.perform(post("/api/v1/gdpr/dpias/{id}/submit-to-dpo", ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void approve_200() throws Exception {
        when(service.approve(eq(ID), any()))
                .thenReturn(view(DpiaStatus.APPROVED, RiskLevel.LOW));
        mockMvc.perform(post("/api/v1/gdpr/dpias/{id}/approve", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dpoUserId\":\"" + DPO + "\",\"dpoOpinion\":\"ok\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void approve_blankOpinion_400() throws Exception {
        mockMvc.perform(post("/api/v1/gdpr/dpias/{id}/approve", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dpoUserId\":\"" + DPO + "\",\"dpoOpinion\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void approve_missingDpoFlag_409() throws Exception {
        when(service.approve(eq(ID), any()))
                .thenThrow(new DpiaStateException("Art. 36 consultation flag required"));
        mockMvc.perform(post("/api/v1/gdpr/dpias/{id}/approve", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dpoUserId\":\"" + DPO + "\",\"dpoOpinion\":\"ok\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void reject_200() throws Exception {
        when(service.reject(eq(ID), any()))
                .thenReturn(view(DpiaStatus.REJECTED, RiskLevel.HIGH));
        mockMvc.perform(post("/api/v1/gdpr/dpias/{id}/reject", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dpoUserId\":\"" + DPO + "\",\"dpoOpinion\":\"no\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void archive_200() throws Exception {
        when(service.archive(ID)).thenReturn(view(DpiaStatus.ARCHIVED, RiskLevel.LOW));
        mockMvc.perform(post("/api/v1/gdpr/dpias/{id}/archive", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/gdpr/dpias/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private DpiaDto.View view(DpiaStatus status, RiskLevel risk) {
        return new DpiaDto.View(
                ID, TENANT, "DPIA-2026-001", "Hiring check", "Pre-employment screening",
                Set.of(UUID.randomUUID()),
                "necessity ok", "risks identified", "mitigations applied",
                risk, risk.requiresPriorConsultation(),
                risk.requiresPriorConsultation() ? "CNIL consultation" : null,
                status,
                status == DpiaStatus.APPROVED || status == DpiaStatus.REJECTED ? DPO : null,
                status == DpiaStatus.APPROVED || status == DpiaStatus.REJECTED ? "ok" : null,
                status == DpiaStatus.APPROVED || status == DpiaStatus.REJECTED ? NOW : null,
                status == DpiaStatus.APPROVED ? NOW : null,
                status == DpiaStatus.ARCHIVED ? NOW.plusSeconds(60) : null,
                USER, USER, NOW, NOW);
    }
}
