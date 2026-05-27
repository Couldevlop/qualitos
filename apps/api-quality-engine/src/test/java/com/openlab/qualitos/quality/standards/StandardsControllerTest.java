package com.openlab.qualitos.quality.standards;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = StandardsController.class)
class StandardsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean StandardsService service;
    @MockitoBean CertificationDossierService dossierService;
    @MockitoBean CertificationBlancService certificationBlancService;
    ObjectMapper om;

    static final UUID STD = UUID.randomUUID();
    static final UUID ADO = UUID.randomUUID();
    static final UUID EV = UUID.randomUUID();
    static final UUID REQ = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID LEAD = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.listStandards(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary())));
        mockMvc.perform(get("/api/v1/standards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("iso-9001"));
    }

    @Test @WithMockUser
    void list_withFilters() throws Exception {
        when(service.listStandards(eq(StandardStatus.PUBLISHED), eq("HLS"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary())));
        mockMvc.perform(get("/api/v1/standards")
                        .param("status", "PUBLISHED").param("family", "HLS"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(service.getStandard(STD)).thenReturn(detail());
        mockMvc.perform(get("/api/v1/standards/{id}", STD)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound() throws Exception {
        when(service.getStandard(STD)).thenThrow(new StandardNotFoundException(STD));
        mockMvc.perform(get("/api/v1/standards/{id}", STD))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getByCode_found() throws Exception {
        when(service.getStandardByCode("iso-9001")).thenReturn(detail());
        mockMvc.perform(get("/api/v1/standards/by-code/{code}", "iso-9001"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void listAdoptions_returns200() throws Exception {
        when(service.listAdoptions(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(adoptionResp(AdoptionStatus.PLANNING))));
        mockMvc.perform(get("/api/v1/standards/adoptions")).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void adopt_returns201() throws Exception {
        when(service.adopt(any())).thenReturn(adoptionResp(AdoptionStatus.PLANNING));
        StandardsDto.AdoptRequest req = new StandardsDto.AdoptRequest(STD, "scope", null, LEAD, "AFNOR");
        mockMvc.perform(post("/api/v1/standards/adoptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void adopt_missingStandardId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/standards/adoptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void adopt_alreadyAdopted_returns409() throws Exception {
        when(service.adopt(any())).thenThrow(new AdoptionConflictException("dup"));
        StandardsDto.AdoptRequest req = new StandardsDto.AdoptRequest(STD, null, null, null, null);
        mockMvc.perform(post("/api/v1/standards/adoptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void getAdoption_found() throws Exception {
        when(service.getAdoption(ADO)).thenReturn(adoptionResp(AdoptionStatus.IN_PROGRESS));
        mockMvc.perform(get("/api/v1/standards/adoptions/{id}", ADO)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getAdoption_notFound() throws Exception {
        when(service.getAdoption(ADO)).thenThrow(new TenantStandardNotFoundException(ADO));
        mockMvc.perform(get("/api/v1/standards/adoptions/{id}", ADO))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_success() throws Exception {
        when(service.updateAdoption(eq(ADO), any())).thenReturn(adoptionResp(AdoptionStatus.IN_PROGRESS));
        mockMvc.perform(patch("/api/v1/standards/adoptions/{id}", ADO).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scopeDescription\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void start_success() throws Exception {
        when(service.startProgress(ADO)).thenReturn(adoptionResp(AdoptionStatus.IN_PROGRESS));
        mockMvc.perform(patch("/api/v1/standards/adoptions/{id}/start", ADO).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test @WithMockUser
    void start_invalid_returns409() throws Exception {
        when(service.startProgress(ADO)).thenThrow(new AdoptionStateException("no"));
        mockMvc.perform(patch("/api/v1/standards/adoptions/{id}/start", ADO).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void certify_success() throws Exception {
        when(service.certify(eq(ADO), any())).thenReturn(adoptionResp(AdoptionStatus.CERTIFIED));
        mockMvc.perform(patch("/api/v1/standards/adoptions/{id}/certify", ADO).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certifiedAt\":\"2026-05-14T10:00:00Z\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void certify_missingDate_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/standards/adoptions/{id}/certify", ADO).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void surveillance_success() throws Exception {
        when(service.markSurveillance(ADO)).thenReturn(adoptionResp(AdoptionStatus.SURVEILLANCE));
        mockMvc.perform(patch("/api/v1/standards/adoptions/{id}/surveillance", ADO).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void withdraw_success() throws Exception {
        when(service.withdraw(ADO)).thenReturn(adoptionResp(AdoptionStatus.WITHDRAWN));
        mockMvc.perform(patch("/api/v1/standards/adoptions/{id}/withdraw", ADO).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_success() throws Exception {
        doNothing().when(service).deleteAdoption(ADO);
        mockMvc.perform(delete("/api/v1/standards/adoptions/{id}", ADO).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void delete_certified_returns409() throws Exception {
        doThrow(new AdoptionStateException("c")).when(service).deleteAdoption(ADO);
        mockMvc.perform(delete("/api/v1/standards/adoptions/{id}", ADO).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void linkEvidence_returns201() throws Exception {
        when(service.linkEvidence(eq(ADO), any())).thenReturn(evidenceResp());
        StandardsDto.LinkEvidenceRequest req = new StandardsDto.LinkEvidenceRequest(
                REQ, EvidenceType.DOCUMENT, UUID.randomUUID(), null, "n", USER);
        mockMvc.perform(post("/api/v1/standards/adoptions/{id}/evidence", ADO).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void linkEvidence_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/standards/adoptions/{id}/evidence", ADO).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void linkEvidence_reqNotFound_returns404() throws Exception {
        when(service.linkEvidence(eq(ADO), any()))
                .thenThrow(new RequirementNotFoundException(REQ));
        StandardsDto.LinkEvidenceRequest req = new StandardsDto.LinkEvidenceRequest(
                REQ, EvidenceType.DOCUMENT, UUID.randomUUID(), null, null, USER);
        mockMvc.perform(post("/api/v1/standards/adoptions/{id}/evidence", ADO).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void listEvidence_returns200() throws Exception {
        when(service.listEvidence(ADO)).thenReturn(List.of(evidenceResp()));
        mockMvc.perform(get("/api/v1/standards/adoptions/{id}/evidence", ADO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(EV.toString()));
    }

    @Test @WithMockUser
    void deleteEvidence_success() throws Exception {
        doNothing().when(service).deleteEvidence(ADO, EV);
        mockMvc.perform(delete("/api/v1/standards/adoptions/{id}/evidence/{eid}", ADO, EV).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void deleteEvidence_notFound_returns404() throws Exception {
        doThrow(new EvidenceNotFoundException(EV)).when(service).deleteEvidence(ADO, EV);
        mockMvc.perform(delete("/api/v1/standards/adoptions/{id}/evidence/{eid}", ADO, EV).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void alignment_returns200() throws Exception {
        when(service.computeAlignment(ADO)).thenReturn(new StandardsDto.AlignmentReport(
                ADO, STD, "iso-9001", 75d, 4, 3, 4, 3, List.of()));
        mockMvc.perform(get("/api/v1/standards/adoptions/{id}/alignment", ADO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(75.0));
    }

    @Test @WithMockUser
    void adopt_missingTenant_returns403() throws Exception {
        when(service.adopt(any())).thenThrow(new MissingTenantContextException());
        StandardsDto.AdoptRequest req = new StandardsDto.AdoptRequest(STD, null, null, null, null);
        mockMvc.perform(post("/api/v1/standards/adoptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser
    void roadmap_returns200() throws Exception {
        when(service.getRoadmap(ADO)).thenReturn(new StandardsDto.RoadmapSummary(
                ADO, 19, 3, 1, 0, 15.0, List.of()));
        mockMvc.perform(get("/api/v1/standards/adoptions/{id}/roadmap", ADO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStages").value(19))
                .andExpect(jsonPath("$.doneStages").value(3));
    }

    @Test @WithMockUser
    void updateStage_returns200() throws Exception {
        when(service.updateStage(eq(ADO), any(), any())).thenReturn(
                new StandardsDto.RoadmapStageResponse(UUID.randomUUID(), 1, "Cadrage", null,
                        "2-4 sem", null, "Direction", "PDCA", StageStatus.IN_PROGRESS, null,
                        null, null, null, null, "go", 1));
        mockMvc.perform(patch("/api/v1/standards/adoptions/{id}/roadmap/{sid}", ADO, UUID.randomUUID())
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"notes\":\"go\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test @WithMockUser
    void auditBlanc_returns200() throws Exception {
        when(service.computeAuditBlanc(ADO)).thenReturn(new StandardsDto.AuditBlancReport(
                ADO, STD, "iso-9001", "ISO 9001:2015", Instant.now(),
                50d, 42, 21, 40, 20, 1, 1, 0, "NON PRÊT — écarts majeurs à corriger", List.of()));
        mockMvc.perform(get("/api/v1/standards/adoptions/{id}/audit-blanc", ADO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readinessScore").value(50.0))
                .andExpect(jsonPath("$.criticalGaps").value(1));
    }

    @Test @WithMockUser
    void generateDossier_returns200() throws Exception {
        when(dossierService.generate(ADO)).thenReturn(new StandardsDto.DossierResponse(
                ADO, "iso-9001", "ISO 9001:2015", Instant.now(),
                "a".repeat(64), "stub-tx-1", "dossier.html", "text/html",
                50d, 15d, 1, "<html>...</html>", "c2lnbmF0dXJl"));
        mockMvc.perform(post("/api/v1/standards/adoptions/{id}/dossier", ADO).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sha256").isNotEmpty())
                .andExpect(jsonPath("$.anchorTxRef").value("stub-tx-1"));
    }

    @Test @WithMockUser
    void certificationBlanc_returns200() throws Exception {
        var stage1 = new StandardsDto.AuditStageResult(1, "Revue documentaire (§4-7)",
                true, 92d, "ok", List.of());
        var stage2 = new StandardsDto.AuditStageResult(2, "Audit terrain (§8-10)",
                true, 88d, "ok", List.of("8.2.1"));
        var cert = new StandardsDto.MockCertificate("QOS-BLANC/ISO-9001/2026/AB12", "iso-9001",
                "SMQ siège", "AFNOR", Instant.now(), Instant.now(), "sous réserve", "simulation");
        when(certificationBlancService.simulate(ADO)).thenReturn(new StandardsDto.CertificationBlancReport(
                ADO, STD, "iso-9001", "ISO 9001:2015", Instant.now(), stage1, stage2,
                0, 1, 2, "CERTIFIABLE_SOUS_RESERVE", "Recommandé sous réserve",
                List.of(), cert, "b".repeat(64), "stub-tx-cb", "c2ln"));
        mockMvc.perform(post("/api/v1/standards/adoptions/{id}/certification-blanc", ADO).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("CERTIFIABLE_SOUS_RESERVE"))
                .andExpect(jsonPath("$.minorNonConformities").value(1))
                .andExpect(jsonPath("$.certificate.certificateNumber").value("QOS-BLANC/ISO-9001/2026/AB12"))
                .andExpect(jsonPath("$.anchorTxRef").value("stub-tx-cb"));
    }

    @Test @WithMockUser
    void documentTemplates_returns200() throws Exception {
        when(service.listDocumentTemplates(STD)).thenReturn(List.of(
                new StandardsDto.DocumentTemplateResponse(UUID.randomUUID(), "ISO9001-POL-Q",
                        "Politique Qualité", DocumentObligation.MANDATORY, "POLICY", "MD",
                        "5.2", "desc", true)));
        mockMvc.perform(get("/api/v1/standards/{id}/document-templates", STD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ISO9001-POL-Q"))
                .andExpect(jsonPath("$[0].format").value("MD"));
    }

    @Test @WithMockUser
    void processTemplates_returns200() throws Exception {
        when(service.listProcessTemplates(STD)).thenReturn(List.of(
                new StandardsDto.ProcessTemplateResponse(UUID.randomUUID(), "PROC-AUDIT",
                        "Audit interne", "desc", "9.2", null, 1)));
        mockMvc.perform(get("/api/v1/standards/{id}/process-templates", STD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mapsToClauses").value("9.2"));
    }

    @Test @WithMockUser
    void revisions_returns200() throws Exception {
        when(service.listRevisions(STD)).thenReturn(List.of(
                new StandardsDto.RevisionResponse(UUID.randomUUID(), "2015", RevisionStatus.CURRENT,
                        LocalDate.of(2015, 9, 15), LocalDate.of(2015, 9, 15), "sum", "impact", "url", 1)));
        mockMvc.perform(get("/api/v1/standards/{id}/revisions", STD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CURRENT"));
    }

    @Test @WithMockUser
    void downloadTemplate_returns200() throws Exception {
        when(service.resolveTemplateUri(STD, EV))
                .thenReturn("/standards/templates/iso-9001/politique-qualite.md");
        mockMvc.perform(get("/api/v1/standards/{id}/document-templates/{tid}/download", STD, EV))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("politique-qualite.md")));
    }

    @Test @WithMockUser
    void downloadTemplate_rejectsPathTraversal_returns404() throws Exception {
        when(service.resolveTemplateUri(STD, EV))
                .thenReturn("/standards/templates/../../application.yml");
        mockMvc.perform(get("/api/v1/standards/{id}/document-templates/{tid}/download", STD, EV))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void documentTemplates_standardNotFound_returns404() throws Exception {
        when(service.listDocumentTemplates(STD)).thenThrow(new StandardNotFoundException(STD));
        mockMvc.perform(get("/api/v1/standards/{id}/document-templates", STD))
                .andExpect(status().isNotFound());
    }

    // helpers
    private StandardsDto.StandardSummary summary() {
        return new StandardsDto.StandardSummary(
                STD, "iso-9001", "ISO 9001:2015", "ISO", "2015",
                "HLS", "all", StandardStatus.PUBLISHED, 36);
    }

    private StandardsDto.StandardDetail detail() {
        return new StandardsDto.StandardDetail(
                STD, "iso-9001", "ISO 9001:2015", "ISO", "2015", null,
                "HLS", "all", null, true, 36, null, StandardStatus.PUBLISHED, List.of());
    }

    private StandardsDto.AdoptionResponse adoptionResp(AdoptionStatus status) {
        return new StandardsDto.AdoptionResponse(
                ADO, TENANT, STD, "iso-9001", "ISO 9001:2015",
                status, null, null, LEAD, "AFNOR", null, null,
                Instant.now(), Instant.now());
    }

    private StandardsDto.EvidenceResponse evidenceResp() {
        return new StandardsDto.EvidenceResponse(
                EV, ADO, REQ, "4.1", EvidenceType.DOCUMENT,
                UUID.randomUUID(), null, "n", USER, Instant.now());
    }
}
