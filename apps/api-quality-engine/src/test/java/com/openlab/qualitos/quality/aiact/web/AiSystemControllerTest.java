package com.openlab.qualitos.quality.aiact.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.aiact.application.AiSystemDto;
import com.openlab.qualitos.quality.aiact.application.AiSystemService;
import com.openlab.qualitos.quality.aiact.domain.AiRiskClassification;
import com.openlab.qualitos.quality.aiact.domain.AiSystemNotFoundException;
import com.openlab.qualitos.quality.aiact.domain.AiSystemRole;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStateException;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStatus;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = AiSystemController.class)
class AiSystemControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AiSystemService service;
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
        when(service.list(null)).thenReturn(List.of(view(AiSystemStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/ai-act/systems"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_filteredByStatus_200() throws Exception {
        when(service.list(AiSystemStatus.IN_USE)).thenReturn(List.of(view(AiSystemStatus.IN_USE)));
        mockMvc.perform(get("/api/v1/ai-act/systems").param("status", "IN_USE"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_invalidStatus_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/systems").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void byRisk_200() throws Exception {
        when(service.listByRiskClassification(AiRiskClassification.HIGH))
                .thenReturn(List.of(view(AiSystemStatus.DRAFT)));
        mockMvc.perform(get("/api/v1/ai-act/systems/by-risk")
                        .param("classification", "HIGH"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void byRisk_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/systems/by-risk").param("classification", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_200() throws Exception {
        when(service.get(ID)).thenReturn(view(AiSystemStatus.DRAFT));
        mockMvc.perform(get("/api/v1/ai-act/systems/{id}", ID))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new AiSystemNotFoundException(ID));
        mockMvc.perform(get("/api/v1/ai-act/systems/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("REF-1")).thenReturn(view(AiSystemStatus.DRAFT));
        mockMvc.perform(get("/api/v1/ai-act/systems/by-reference").param("reference", "REF-1"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/systems/by-reference")
                        .param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void draft_201() throws Exception {
        when(service.draft(any())).thenReturn(view(AiSystemStatus.DRAFT));
        AiSystemWebDto.DraftRequest req = new AiSystemWebDto.DraftRequest(
                "REF-1", "Name", null, null, "purpose",
                AiRiskClassification.LIMITED, AiSystemRole.PROVIDER, false,
                null, null, null, "transparency note", null,
                null, Set.of(), Set.of(), USER);
        mockMvc.perform(post("/api/v1/ai-act/systems").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void draft_invalidRef_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"name\":\"N\",\"intendedPurpose\":\"p\","
                + "\"riskClassification\":\"LIMITED\",\"role\":\"PROVIDER\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/systems").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_missingRisk_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"name\":\"N\",\"intendedPurpose\":\"p\","
                + "\"role\":\"PROVIDER\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/systems").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_missingActor_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"name\":\"N\",\"intendedPurpose\":\"p\","
                + "\"riskClassification\":\"LIMITED\",\"role\":\"PROVIDER\"}";
        mockMvc.perform(post("/api/v1/ai-act/systems").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void draft_duplicate_409() throws Exception {
        when(service.draft(any())).thenThrow(new AiSystemStateException("Reference already used"));
        AiSystemWebDto.DraftRequest req = new AiSystemWebDto.DraftRequest(
                "REF-DUP", "Name", null, null, "purpose",
                AiRiskClassification.LIMITED, AiSystemRole.PROVIDER, false,
                null, null, null, "t", null, null, Set.of(), Set.of(), USER);
        mockMvc.perform(post("/api/v1/ai-act/systems").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(AiSystemStatus.DRAFT));
        AiSystemWebDto.EditRequest req = new AiSystemWebDto.EditRequest(
                "Updated", null, null, "new purpose",
                AiRiskClassification.MINIMAL_OR_NO, AiSystemRole.DEPLOYER, true,
                null, null, null, null, null, null, Set.of(), Set.of());
        mockMvc.perform(put("/api/v1/ai-act/systems/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void register_200() throws Exception {
        when(service.register(ID)).thenReturn(view(AiSystemStatus.REGISTERED));
        mockMvc.perform(post("/api/v1/ai-act/systems/{id}/register", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void register_unacceptable_409() throws Exception {
        when(service.register(ID))
                .thenThrow(new AiSystemStateException("Cannot register an AI system classified UNACCEPTABLE"));
        mockMvc.perform(post("/api/v1/ai-act/systems/{id}/register", ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void putInUse_200() throws Exception {
        when(service.putInUse(ID)).thenReturn(view(AiSystemStatus.IN_USE));
        mockMvc.perform(post("/api/v1/ai-act/systems/{id}/put-in-use", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void putInUse_missingEvidence_409() throws Exception {
        when(service.putInUse(ID))
                .thenThrow(new AiSystemStateException("HIGH risk requires conformityAssessmentEvidenceUrl"));
        mockMvc.perform(post("/api/v1/ai-act/systems/{id}/put-in-use", ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void decommission_200() throws Exception {
        when(service.decommission(ID)).thenReturn(view(AiSystemStatus.DECOMMISSIONED));
        mockMvc.perform(post("/api/v1/ai-act/systems/{id}/decommission", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void withdraw_200() throws Exception {
        when(service.withdraw(eq(ID), any())).thenReturn(view(AiSystemStatus.WITHDRAWN));
        mockMvc.perform(post("/api/v1/ai-act/systems/{id}/withdraw", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"plans changed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void withdraw_blankReason_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/systems/{id}/withdraw", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/ai-act/systems/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private AiSystemDto.View view(AiSystemStatus status) {
        return new AiSystemDto.View(
                ID, TENANT, "REF-1", "Name", null,
                "Provider", "purpose",
                AiRiskClassification.LIMITED, AiSystemRole.PROVIDER, false,
                status,
                null, null, null, "t", null,
                null, Set.of(), Set.of(),
                status == AiSystemStatus.IN_USE ? NOW : null,
                status == AiSystemStatus.DECOMMISSIONED || status == AiSystemStatus.WITHDRAWN
                        ? NOW : null,
                status == AiSystemStatus.WITHDRAWN ? "reason" : null,
                USER, NOW, NOW,
                false, false, true);
    }
}
