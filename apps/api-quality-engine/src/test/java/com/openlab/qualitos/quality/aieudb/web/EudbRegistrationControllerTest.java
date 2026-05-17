package com.openlab.qualitos.quality.aieudb.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.aieudb.application.EudbRegistrationDto;
import com.openlab.qualitos.quality.aieudb.application.EudbRegistrationService;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationNotFoundException;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStateException;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStatus;
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
@WebMvcTest(controllers = EudbRegistrationController.class)
class EudbRegistrationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean EudbRegistrationService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-17T10:00:00Z");

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(null)).thenReturn(List.of(view(EudbRegistrationStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/ai-act/eudb")).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_byStatus_200() throws Exception {
        when(service.list(EudbRegistrationStatus.REGISTERED))
                .thenReturn(List.of(view(EudbRegistrationStatus.REGISTERED)));
        mockMvc.perform(get("/api/v1/ai-act/eudb").param("status", "REGISTERED"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_invalidStatus_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/eudb").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void bySystem_200() throws Exception {
        when(service.listByAiSystem(SYS)).thenReturn(List.of(view(EudbRegistrationStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/ai-act/eudb/by-system").param("aiSystemId", SYS.toString()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_200() throws Exception {
        when(service.get(ID)).thenReturn(view(EudbRegistrationStatus.DRAFT));
        mockMvc.perform(get("/api/v1/ai-act/eudb/{id}", ID)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new EudbRegistrationNotFoundException(ID));
        mockMvc.perform(get("/api/v1/ai-act/eudb/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("REF-1")).thenReturn(view(EudbRegistrationStatus.DRAFT));
        mockMvc.perform(get("/api/v1/ai-act/eudb/by-reference").param("reference", "REF-1"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/eudb/by-reference").param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void getByEudbId_200() throws Exception {
        when(service.getByEudbId("EUDB-AI-ABC123"))
                .thenReturn(view(EudbRegistrationStatus.REGISTERED));
        mockMvc.perform(get("/api/v1/ai-act/eudb/by-eudb-id").param("eudbId", "EUDB-AI-ABC123"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByEudbId_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/eudb/by-eudb-id").param("eudbId", "invalid"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void draft_201() throws Exception {
        when(service.draft(any())).thenReturn(view(EudbRegistrationStatus.DRAFT));
        EudbRegistrationWebDto.DraftRequest req = new EudbRegistrationWebDto.DraftRequest(
                "REF-1", SYS, "Acme", "EU Rep", "FR", "purpose", "doc", USER);
        mockMvc.perform(post("/api/v1/ai-act/eudb").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void draft_invalidRef_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"aiSystemId\":\"" + SYS + "\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/eudb").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_invalidMemberState_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"aiSystemId\":\"" + SYS + "\","
                + "\"memberStateOfReference\":\"France\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/eudb").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_missingActor_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"aiSystemId\":\"" + SYS + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/eudb").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_duplicate_409() throws Exception {
        when(service.draft(any()))
                .thenThrow(new EudbRegistrationStateException("Reference already used"));
        EudbRegistrationWebDto.DraftRequest req = new EudbRegistrationWebDto.DraftRequest(
                "REF-DUP", SYS, "Acme", null, "FR", "purpose", null, USER);
        mockMvc.perform(post("/api/v1/ai-act/eudb").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(EudbRegistrationStatus.DRAFT));
        EudbRegistrationWebDto.EditRequest req = new EudbRegistrationWebDto.EditRequest(
                "Acme 2", null, "DE", "new", null);
        mockMvc.perform(put("/api/v1/ai-act/eudb/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void submit_200() throws Exception {
        when(service.submit(eq(ID), any())).thenReturn(view(EudbRegistrationStatus.SUBMITTED));
        mockMvc.perform(post("/api/v1/ai-act/eudb/{id}/submit", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submittedByUserId\":\"" + USER + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void markRegistered_200() throws Exception {
        when(service.markRegistered(eq(ID), any()))
                .thenReturn(view(EudbRegistrationStatus.REGISTERED));
        EudbRegistrationWebDto.MarkRegisteredRequest req =
                new EudbRegistrationWebDto.MarkRegisteredRequest("EUDB-AI-ABC123", NOW);
        mockMvc.perform(post("/api/v1/ai-act/eudb/{id}/mark-registered", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void markRegistered_invalidEudbId_400() throws Exception {
        String body = "{\"eudbId\":\"invalid\",\"registrationDate\":\"" + NOW + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/eudb/{id}/mark-registered", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void declareUpdate_200() throws Exception {
        when(service.declareUpdate(eq(ID), any()))
                .thenReturn(view(EudbRegistrationStatus.UPDATED));
        EudbRegistrationWebDto.DeclareUpdateRequest req =
                new EudbRegistrationWebDto.DeclareUpdateRequest("retrained", NOW);
        mockMvc.perform(post("/api/v1/ai-act/eudb/{id}/declare-update", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void reject_200() throws Exception {
        when(service.reject(eq(ID), any())).thenReturn(view(EudbRegistrationStatus.REJECTED));
        mockMvc.perform(post("/api/v1/ai-act/eudb/{id}/reject", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"incomplete\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void reject_blankReason_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/eudb/{id}/reject", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void retire_200() throws Exception {
        when(service.retire(eq(ID), any())).thenReturn(view(EudbRegistrationStatus.RETIRED));
        mockMvc.perform(post("/api/v1/ai-act/eudb/{id}/retire", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"EOL\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/ai-act/eudb/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private EudbRegistrationDto.View view(EudbRegistrationStatus status) {
        return new EudbRegistrationDto.View(
                ID, TENANT, "REF-1", SYS,
                "Acme", "EU Rep", "FR", "purpose", "doc",
                status == EudbRegistrationStatus.REGISTERED
                        || status == EudbRegistrationStatus.UPDATED
                        || status == EudbRegistrationStatus.RETIRED ? "EUDB-AI-ABC123" : null,
                status,
                status != EudbRegistrationStatus.DRAFT
                        && status != EudbRegistrationStatus.REJECTED ? NOW : null,
                status != EudbRegistrationStatus.DRAFT
                        && status != EudbRegistrationStatus.REJECTED ? USER : null,
                status == EudbRegistrationStatus.REGISTERED
                        || status == EudbRegistrationStatus.UPDATED
                        || status == EudbRegistrationStatus.RETIRED ? NOW : null,
                status == EudbRegistrationStatus.UPDATED ? NOW : null,
                status == EudbRegistrationStatus.UPDATED ? "retrained" : null,
                status == EudbRegistrationStatus.REJECTED ? NOW : null,
                status == EudbRegistrationStatus.REJECTED ? "incomplete" : null,
                status == EudbRegistrationStatus.RETIRED ? NOW : null,
                status == EudbRegistrationStatus.RETIRED ? "EOL" : null,
                USER, NOW, NOW);
    }
}
