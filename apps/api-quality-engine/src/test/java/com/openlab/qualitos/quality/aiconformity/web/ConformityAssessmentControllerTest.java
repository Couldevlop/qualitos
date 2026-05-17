package com.openlab.qualitos.quality.aiconformity.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.aiconformity.application.ConformityAssessmentDto;
import com.openlab.qualitos.quality.aiconformity.application.ConformityAssessmentService;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentNotFoundException;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStateException;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStatus;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityProcedure;
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
@WebMvcTest(controllers = ConformityAssessmentController.class)
class ConformityAssessmentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ConformityAssessmentService service;
    ObjectMapper om;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final UUID QMS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-17T10:00:00Z");
    static final Instant VALID_UNTIL = NOW.plusSeconds(365L * 86400);

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(null)).thenReturn(List.of(view(ConformityAssessmentStatus.PLANNED)));
        mockMvc.perform(get("/api/v1/ai-act/conformity-assessments"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_byStatus_200() throws Exception {
        when(service.list(ConformityAssessmentStatus.CERTIFIED))
                .thenReturn(List.of(view(ConformityAssessmentStatus.CERTIFIED)));
        mockMvc.perform(get("/api/v1/ai-act/conformity-assessments").param("status", "CERTIFIED"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_invalidStatus_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/conformity-assessments").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void bySystem_200() throws Exception {
        when(service.listByAiSystem(SYS)).thenReturn(List.of(view(ConformityAssessmentStatus.PLANNED)));
        mockMvc.perform(get("/api/v1/ai-act/conformity-assessments/by-system")
                        .param("aiSystemId", SYS.toString()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void expiring_200() throws Exception {
        when(service.listExpiringCertificates(200))
                .thenReturn(List.of(view(ConformityAssessmentStatus.CERTIFIED)));
        mockMvc.perform(get("/api/v1/ai-act/conformity-assessments/expiring-certificates"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void expiring_outOfRange_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/conformity-assessments/expiring-certificates")
                        .param("limit", "0"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void get_200() throws Exception {
        when(service.get(ID)).thenReturn(view(ConformityAssessmentStatus.PLANNED));
        mockMvc.perform(get("/api/v1/ai-act/conformity-assessments/{id}", ID))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new ConformityAssessmentNotFoundException(ID));
        mockMvc.perform(get("/api/v1/ai-act/conformity-assessments/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByReference_200() throws Exception {
        when(service.getByReference("REF-1"))
                .thenReturn(view(ConformityAssessmentStatus.PLANNED));
        mockMvc.perform(get("/api/v1/ai-act/conformity-assessments/by-reference")
                        .param("reference", "REF-1"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getByReference_invalid_400() throws Exception {
        mockMvc.perform(get("/api/v1/ai-act/conformity-assessments/by-reference")
                        .param("reference", "lowercase"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void plan_201() throws Exception {
        when(service.plan(any())).thenReturn(view(ConformityAssessmentStatus.PLANNED));
        ConformityAssessmentWebDto.PlanRequest req = new ConformityAssessmentWebDto.PlanRequest(
                "REF-1", SYS, QMS, ConformityProcedure.INTERNAL_CONTROL,
                null, null, "scope", USER);
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void plan_invalidRef_400() throws Exception {
        String body = "{\"reference\":\"lowercase\",\"aiSystemId\":\"" + SYS + "\","
                + "\"procedure\":\"INTERNAL_CONTROL\",\"scope\":\"s\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void plan_invalidNotifiedBodyId_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"aiSystemId\":\"" + SYS + "\","
                + "\"procedure\":\"NOTIFIED_BODY\",\"notifiedBodyId\":\"abc\","
                + "\"notifiedBodyName\":\"Body\",\"scope\":\"s\","
                + "\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void plan_missingProcedure_400() throws Exception {
        String body = "{\"reference\":\"REF-1\",\"aiSystemId\":\"" + SYS + "\","
                + "\"scope\":\"s\",\"createdByUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void plan_notifiedWithoutBody_409() throws Exception {
        when(service.plan(any())).thenThrow(new ConformityAssessmentStateException(
                "NOTIFIED_BODY procedure requires notifiedBodyId and notifiedBodyName"));
        ConformityAssessmentWebDto.PlanRequest req = new ConformityAssessmentWebDto.PlanRequest(
                "REF-X", SYS, QMS, ConformityProcedure.NOTIFIED_BODY,
                null, null, "scope", USER);
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void edit_200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(ConformityAssessmentStatus.PLANNED));
        ConformityAssessmentWebDto.EditRequest req = new ConformityAssessmentWebDto.EditRequest(
                QMS, null, null, "new scope");
        mockMvc.perform(put("/api/v1/ai-act/conformity-assessments/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void start_200() throws Exception {
        when(service.start(ID)).thenReturn(view(ConformityAssessmentStatus.IN_PROGRESS));
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments/{id}/start", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void certify_200() throws Exception {
        when(service.certify(eq(ID), any())).thenReturn(view(ConformityAssessmentStatus.CERTIFIED));
        ConformityAssessmentWebDto.CertifyRequest req =
                new ConformityAssessmentWebDto.CertifyRequest("CERT-1", "EU-1", VALID_UNTIL);
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments/{id}/certify", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void certify_missingCert_400() throws Exception {
        String body = "{\"euDeclarationReference\":\"EU-1\",\"validUntil\":\""
                + VALID_UNTIL + "\"}";
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments/{id}/certify", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void markExpired_200() throws Exception {
        when(service.markExpired(ID)).thenReturn(view(ConformityAssessmentStatus.EXPIRED));
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments/{id}/mark-expired", ID)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void revoke_200() throws Exception {
        when(service.revoke(eq(ID), any())).thenReturn(view(ConformityAssessmentStatus.REVOKED));
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments/{id}/revoke", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"misuse\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void revoke_blankReason_400() throws Exception {
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments/{id}/revoke", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void markFailed_200() throws Exception {
        when(service.markFailed(eq(ID), any())).thenReturn(view(ConformityAssessmentStatus.FAILED));
        mockMvc.perform(post("/api/v1/ai-act/conformity-assessments/{id}/fail", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"non-conformities\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/ai-act/conformity-assessments/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private ConformityAssessmentDto.View view(ConformityAssessmentStatus status) {
        return new ConformityAssessmentDto.View(
                ID, TENANT, "REF-1", SYS, QMS,
                ConformityProcedure.INTERNAL_CONTROL,
                null, null, "scope", status,
                NOW,
                status != ConformityAssessmentStatus.PLANNED
                        && status != ConformityAssessmentStatus.FAILED ? NOW : null,
                status == ConformityAssessmentStatus.CERTIFIED
                        || status == ConformityAssessmentStatus.EXPIRED ? NOW : null,
                status == ConformityAssessmentStatus.CERTIFIED
                        || status == ConformityAssessmentStatus.EXPIRED ? "CERT-1" : null,
                status == ConformityAssessmentStatus.CERTIFIED
                        || status == ConformityAssessmentStatus.EXPIRED ? VALID_UNTIL : null,
                status == ConformityAssessmentStatus.CERTIFIED
                        || status == ConformityAssessmentStatus.EXPIRED ? "EU-1" : null,
                status == ConformityAssessmentStatus.EXPIRED ? VALID_UNTIL.plusSeconds(1) : null,
                status == ConformityAssessmentStatus.REVOKED ? NOW : null,
                status == ConformityAssessmentStatus.REVOKED ? "misuse" : null,
                status == ConformityAssessmentStatus.FAILED ? NOW : null,
                status == ConformityAssessmentStatus.FAILED ? "non-conformities" : null,
                USER, NOW, NOW);
    }
}
