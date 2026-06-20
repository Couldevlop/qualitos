package com.openlab.qualitos.quality.standards.normdoc.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.standards.StandardNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocDto;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocService;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStateException;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = NormDocController.class)
@Import({MethodSecurityTestConfig.class, NormDocExceptionHandler.class})
class NormDocControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean NormDocService service;
    ObjectMapper om;

    static final UUID ID = UUID.randomUUID();
    static final UUID STD = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private NormDocDto.View view(NormDocStatus status) {
        return new NormDocDto.View(ID, TENANT, STD, "iso-9001", NormDocKind.MANUAL,
                "Manuel Qualité — ACME (iso-9001)",
                List.of(new NormDocDto.SectionView("ctx", "Contexte", List.of("4.1"), "Corps")),
                status, "ollama", "# Manuel\n",
                null, null, null, null, null, null, null,
                USER, Instant.now(), Instant.now());
    }

    private Map<String, Object> generateBody() {
        return Map.of(
                "standardId", STD.toString(),
                "kind", "MANUAL",
                "tenantProfile", Map.of(
                        "organizationName", "ACME",
                        "industry", "manufacturing",
                        "size", "PME",
                        "language", "fr",
                        "knownProcesses", List.of("achats")),
                "sections", List.of(Map.of(
                        "key", "ctx", "title", "Contexte",
                        "clauses", List.of("4.1"), "guidance", "cadrer")));
    }

    // ---- generate ----

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void generate_returns201() throws Exception {
        when(service.generate(any())).thenReturn(view(NormDocStatus.BROUILLON_IA));
        mockMvc.perform(post("/api/v1/standards/norm-documents/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(generateBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BROUILLON_IA"))
                .andExpect(jsonPath("$.standardCode").value("iso-9001"));
    }

    @Test @WithMockUser(roles = "USER")
    void generate_insufficientRole_403() throws Exception {
        mockMvc.perform(post("/api/v1/standards/norm-documents/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(generateBody())))
                .andExpect(status().isForbidden());
    }

    @Test
    void generate_unauthenticated_401() throws Exception {
        mockMvc.perform(post("/api/v1/standards/norm-documents/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(generateBody())))
                .andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void generate_invalidBody_400() throws Exception {
        mockMvc.perform(post("/api/v1/standards/norm-documents/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void generate_unknownStandard_404() throws Exception {
        when(service.generate(any())).thenThrow(new StandardNotFoundException(STD));
        mockMvc.perform(post("/api/v1/standards/norm-documents/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(generateBody())))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void generate_serviceIllegalArgument_400() throws Exception {
        when(service.generate(any()))
                .thenThrow(new IllegalArgumentException("tenantProfile required"));
        mockMvc.perform(post("/api/v1/standards/norm-documents/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(generateBody())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("tenantProfile required"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void generate_nullClausesAndProcesses_mappedToEmpty() throws Exception {
        // clauses/knownProcesses absents → le contrôleur substitue des listes vides
        // (couvre les branches null des ternaires de mapping).
        when(service.generate(any())).thenReturn(view(NormDocStatus.BROUILLON_IA));
        Map<String, Object> body = Map.of(
                "standardId", STD.toString(),
                "kind", "MANUAL",
                "tenantProfile", Map.of(
                        "organizationName", "ACME", "industry", "it", "size", "PME"),
                "sections", List.of(Map.of("key", "s", "title", "Champ")));
        mockMvc.perform(post("/api/v1/standards/norm-documents/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    // ---- list / get ----

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.list(eq(NormDocStatus.BROUILLON_IA)))
                .thenReturn(List.of(view(NormDocStatus.BROUILLON_IA)));
        mockMvc.perform(get("/api/v1/standards/norm-documents").param("status", "BROUILLON_IA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("MANUAL"));
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(service.get(ID)).thenReturn(view(NormDocStatus.BROUILLON_IA));
        mockMvc.perform(get("/api/v1/standards/norm-documents/{id}", ID))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new NormDocNotFoundException(ID));
        mockMvc.perform(get("/api/v1/standards/norm-documents/{id}", ID))
                .andExpect(status().isNotFound());
    }

    // ---- edit ----

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void edit_returns200() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(NormDocStatus.BROUILLON_IA));
        Map<String, Object> body = Map.of(
                "title", "Titre",
                "sections", List.of(Map.of(
                        "key", "ctx", "title", "Contexte",
                        "clauses", List.of("4.1"), "bodyMarkdown", "Corps")));
        mockMvc.perform(put("/api/v1/standards/norm-documents/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void edit_nullClauses_mappedToEmpty() throws Exception {
        when(service.edit(eq(ID), any())).thenReturn(view(NormDocStatus.BROUILLON_IA));
        Map<String, Object> body = Map.of(
                "title", "Titre",
                // clauses absent → branche null du mapping section.
                "sections", List.of(Map.of(
                        "key", "ctx", "title", "Contexte", "bodyMarkdown", "Corps")));
        mockMvc.perform(put("/api/v1/standards/norm-documents/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    // ---- submit ----

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void submit_returns200() throws Exception {
        when(service.submitForReview(ID)).thenReturn(view(NormDocStatus.EN_VALIDATION));
        mockMvc.perform(post("/api/v1/standards/norm-documents/{id}/submit", ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_VALIDATION"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void submit_invalidState_409() throws Exception {
        when(service.submitForReview(ID))
                .thenThrow(new NormDocStateException("Transition not allowed"));
        mockMvc.perform(post("/api/v1/standards/norm-documents/{id}/submit", ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    // ---- approve ----

    @Test @WithMockUser(roles = "DIRECTOR_QUALITY")
    void approve_returns200() throws Exception {
        when(service.approve(eq(ID), any())).thenReturn(view(NormDocStatus.APPROUVE));
        mockMvc.perform(post("/api/v1/standards/norm-documents/{id}/approve", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "signature", "sig-humaine", "notes", "OK"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROUVE"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void approve_managerForbidden_403() throws Exception {
        mockMvc.perform(post("/api/v1/standards/norm-documents/{id}/approve", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("signature", "sig"))))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "DIRECTOR_QUALITY")
    void approve_missingSignature_400() throws Exception {
        mockMvc.perform(post("/api/v1/standards/norm-documents/{id}/approve", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("notes", "x"))))
                .andExpect(status().isBadRequest());
    }

    // ---- reject ----

    @Test @WithMockUser(roles = "DIRECTOR_QUALITY")
    void reject_returns200() throws Exception {
        when(service.reject(eq(ID), any())).thenReturn(view(NormDocStatus.BROUILLON_IA));
        mockMvc.perform(post("/api/v1/standards/norm-documents/{id}/reject", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("reason", "Sections vides"))))
                .andExpect(status().isOk());
    }

    // ---- delete ----

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/standards/norm-documents/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void delete_approvedConflict_409() throws Exception {
        org.mockito.Mockito.doThrow(new NormDocStateException("Approved cannot be deleted"))
                .when(service).delete(ID);
        mockMvc.perform(delete("/api/v1/standards/norm-documents/{id}", ID).with(csrf()))
                .andExpect(status().isConflict());
    }
}
