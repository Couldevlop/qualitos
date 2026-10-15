package com.openlab.qualitos.quality.capa;

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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = CapaController.class)
class CapaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CapaService service;
    ObjectMapper om;

    static final UUID CAPA = UUID.randomUUID();
    static final UUID ACTION = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();
    static final UUID NC = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void list_returns200() throws Exception {
        when(service.findAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(caseResp(CapaStatus.OPEN))));
        mockMvc.perform(get("/api/v1/capa/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(CAPA.toString()));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void open_creeLeDossierEtSonAction_avecLaSourceChoisie() throws Exception {
        when(service.openCase(any(), eq(CapaSourceType.COMPLAINT), eq("REC-12")))
                .thenReturn(caseResp(CapaStatus.OPEN));
        mockMvc.perform(post("/api/v1/capa/cases/open").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(openReq(CapaSourceType.COMPLAINT, "REC-12"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CAPA.toString()));
        verify(service).openCase(any(), eq(CapaSourceType.COMPLAINT), eq("REC-12"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void open_sansSource_estUneDetectionInterne() throws Exception {
        when(service.openCase(any(), eq(CapaSourceType.INTERNAL), isNull()))
                .thenReturn(caseResp(CapaStatus.OPEN));
        mockMvc.perform(post("/api/v1/capa/cases/open").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(openReq(null, null))))
                .andExpect(status().isCreated());
        verify(service).openCase(any(), eq(CapaSourceType.INTERNAL), isNull());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void open_laDecisionDeVerificationParvientAuService() throws Exception {
        UUID verificateur = UUID.randomUUID();
        when(service.openCase(any(), eq(CapaSourceType.INTERNAL), isNull()))
                .thenReturn(caseResp(CapaStatus.OPEN));
        CapaDto.OpenCaseRequest base = openReq(null, null);
        CapaDto.OpenCaseRequest demande = new CapaDto.OpenCaseRequest(base.title(), null, base.type(),
                base.criticity(), null, null, base.ownerId(), base.ownerName(), base.dueDate(),
                true, verificateur, "Claire Contrôle", "Relever 20 pièces");

        mockMvc.perform(post("/api/v1/capa/cases/open").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(demande)))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<CapaDto.OpenCaseRequest> captor =
                org.mockito.ArgumentCaptor.forClass(CapaDto.OpenCaseRequest.class);
        verify(service).openCase(captor.capture(), eq(CapaSourceType.INTERNAL), isNull());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().verificationRequired()).isTrue();
        org.assertj.core.api.Assertions.assertThat(captor.getValue().verificationAssigneeId())
                .isEqualTo(verificateur);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().verificationAssigneeName())
                .isEqualTo("Claire Contrôle");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().verificationInstructions())
                .isEqualTo("Relever 20 pièces");
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void open_sourceRisque_estRefusee_seulLeRegistreLaPose() throws Exception {
        mockMvc.perform(post("/api/v1/capa/cases/open").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(openReq(CapaSourceType.RISK, "R-001"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid CAPA Input"));
        verify(service, never()).openCase(any(), any(), any());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void open_sansResponsableNiEcheance_estRefuseAvantLeService() throws Exception {
        mockMvc.perform(post("/api/v1/capa/cases/open").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"type\":\"CORRECTIVE\",\"criticity\":\"HIGH\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).openCase(any(), any(), any());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void list_withFilter() throws Exception {
        when(service.findAll(eq(CapaStatus.CLOSED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(caseResp(CapaStatus.CLOSED))));
        mockMvc.perform(get("/api/v1/capa/cases").param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("CLOSED"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void create_returns201() throws Exception {
        when(service.createCase(any())).thenReturn(caseResp(CapaStatus.OPEN));
        CapaDto.CreateCaseRequest req = new CapaDto.CreateCaseRequest(
                "t", null, CapaType.CORRECTIVE, CapaCriticity.HIGH,
                CapaSourceType.NON_CONFORMITY, null, OWNER, null, null);
        mockMvc.perform(post("/api/v1/capa/cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CAPA.toString()));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void create_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/capa/cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CORRECTIVE\",\"criticity\":\"HIGH\",\"sourceType\":\"INTERNAL\",\"ownerId\":\""
                                + OWNER + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void create_missingType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/capa/cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"criticity\":\"HIGH\",\"sourceType\":\"INTERNAL\",\"ownerId\":\""
                                + OWNER + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void get_found() throws Exception {
        when(service.findById(CAPA)).thenReturn(caseResp(CapaStatus.OPEN));
        mockMvc.perform(get("/api/v1/capa/cases/{id}", CAPA)).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void get_notFound() throws Exception {
        when(service.findById(CAPA)).thenThrow(new CapaNotFoundException(CAPA));
        mockMvc.perform(get("/api/v1/capa/cases/{id}", CAPA)).andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void update_success() throws Exception {
        when(service.updateCase(eq(CAPA), any())).thenReturn(caseResp(CapaStatus.OPEN));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}", CAPA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void start_success() throws Exception {
        when(service.startCase(CAPA)).thenReturn(caseResp(CapaStatus.IN_PROGRESS));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/start", CAPA).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void start_invalid_returns409() throws Exception {
        when(service.startCase(CAPA)).thenThrow(new CapaStateException("nope"));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/start", CAPA).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void resolve_success() throws Exception {
        when(service.resolveCase(CAPA)).thenReturn(caseResp(CapaStatus.RESOLVED));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/resolve", CAPA).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void effectiveness_success() throws Exception {
        when(service.verifyEffectiveness(eq(CAPA), any())).thenReturn(caseResp(CapaStatus.CLOSED));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/effectiveness", CAPA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"effective\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void effectiveness_missingField_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/effectiveness", CAPA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void reject_success() throws Exception {
        when(service.rejectCase(CAPA)).thenReturn(caseResp(CapaStatus.REJECTED));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/reject", CAPA).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void delete_success() throws Exception {
        doNothing().when(service).deleteCase(CAPA);
        mockMvc.perform(delete("/api/v1/capa/cases/{id}", CAPA).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void delete_closed_returns409() throws Exception {
        doThrow(new CapaStateException("c")).when(service).deleteCase(CAPA);
        mockMvc.perform(delete("/api/v1/capa/cases/{id}", CAPA).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void addAction_success() throws Exception {
        when(service.addAction(eq(CAPA), any())).thenReturn(actionResp());
        CapaDto.ActionRequest req = new CapaDto.ActionRequest("a", null, null, null, null, null, null, null);
        mockMvc.perform(post("/api/v1/capa/cases/{id}/actions", CAPA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ACTION.toString()));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void addAction_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/capa/cases/{id}/actions", CAPA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void updateAction_success() throws Exception {
        when(service.updateAction(eq(CAPA), eq(ACTION), any())).thenReturn(actionResp());
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/actions/{aid}", CAPA, ACTION).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void updateAction_notFound_returns404() throws Exception {
        when(service.updateAction(eq(CAPA), eq(ACTION), any()))
                .thenThrow(new CapaActionNotFoundException(ACTION));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/actions/{aid}", CAPA, ACTION).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void updateAction_editionEnLigne_titreEtStatut_retourne200() throws Exception {
        // L'édition en ligne du tableau n'envoie que ce qu'elle change.
        when(service.updateAction(eq(CAPA), eq(ACTION), any())).thenReturn(actionResp());
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/actions/{aid}", CAPA, ACTION).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Libellé corrigé\",\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void updateAction_libelleVide_retourne400_etNon500() throws Exception {
        // Le PATCH n'est pas validé par Jakarta (un champ absent doit rester
        // intouché) : le refus vient du service, et doit sortir en 400 — sinon
        // l'API annoncerait une panne là où l'utilisateur a vidé un champ.
        when(service.updateAction(eq(CAPA), eq(ACTION), any()))
                .thenThrow(new CapaValidationException("Action title must not be blank"));
        mockMvc.perform(patch("/api/v1/capa/cases/{id}/actions/{aid}", CAPA, ACTION).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid CAPA Input"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void get_exposeLEcartDOrigine_pourLaColonneNonConformite() throws Exception {
        when(service.findById(CAPA)).thenReturn(new CapaDto.CaseResponse(
                CAPA, TENANT, "t", null, CapaType.CORRECTIVE, CapaCriticity.HIGH, CapaStatus.OPEN,
                CapaSourceType.NON_CONFORMITY, "NC-2026-0018", OWNER, "a.diallo@client.test", null, null,
                null, null, null, null,
                null, null, null, null,
                Instant.now(), Instant.now(), List.of(),
                new CapaDto.LinkedNonConformity(NC, "NC-2026-0018", "Étiquetage lot 4471 illisible"), List.of()));

        mockMvc.perform(get("/api/v1/capa/cases/{id}", CAPA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceNonConformity.title")
                        .value("Étiquetage lot 4471 illisible"))
                .andExpect(jsonPath("$.sourceNonConformity.reference").value("NC-2026-0018"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void deleteAction_success() throws Exception {
        doNothing().when(service).deleteAction(CAPA, ACTION);
        mockMvc.perform(delete("/api/v1/capa/cases/{id}/actions/{aid}", CAPA, ACTION).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void create_missingTenant_returns403() throws Exception {
        when(service.createCase(any())).thenThrow(new MissingTenantContextException());
        CapaDto.CreateCaseRequest req = new CapaDto.CreateCaseRequest(
                "t", null, CapaType.CORRECTIVE, CapaCriticity.HIGH,
                CapaSourceType.NON_CONFORMITY, null, OWNER, null, null);
        mockMvc.perform(post("/api/v1/capa/cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // helpers
    private CapaDto.OpenCaseRequest openReq(CapaSourceType source, String ref) {
        return new CapaDto.OpenCaseRequest("Recalibrer la sonde", null, CapaType.CORRECTIVE,
                CapaCriticity.HIGH, source, ref, OWNER, "a.diallo@client.test",
                java.time.LocalDate.now().plusDays(10), null, null, null, null);
    }

    private CapaDto.CaseResponse caseResp(CapaStatus s) {
        return new CapaDto.CaseResponse(
                CAPA, TENANT, "t", null, CapaType.CORRECTIVE, CapaCriticity.HIGH, s,
                CapaSourceType.NON_CONFORMITY, null, OWNER, null, null, null,
                null, null, null, null,
                null, null, null, null,
                Instant.now(), Instant.now(), List.of(), null, List.of());
    }

    private CapaDto.ActionResponse actionResp() {
        return new CapaDto.ActionResponse(
                ACTION, CAPA, "a", null, CapaActionStatus.PENDING, CapaActionType.CORRECTIVE,
                null, null, null, null, null, Instant.now(), Instant.now());
    }
}
