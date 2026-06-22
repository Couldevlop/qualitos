package com.openlab.qualitos.quality.standards.normdoc.dossier.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.standards.StandardNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierDto;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierService;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierStateException;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierStatus;
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
@WebMvcTest(controllers = DossierController.class)
@Import({MethodSecurityTestConfig.class, DossierExceptionHandler.class})
class DossierControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DossierService service;
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

    private DossierDto.View view(DossierStatus status) {
        return new DossierDto.View(ID, TENANT, STD, "iso-9001", "ISO 9001:2015", "ACME", "fr",
                status, "ollama",
                List.of(new DossierDto.DocumentView("manuel-qualite", NormDocKind.MANUAL, "Manuel",
                        DossierDocStatus.GENERE, UUID.randomUUID(), null, null, 7)),
                1, 1, 0, 100,
                null, null, null, null, null, USER, Instant.now(), Instant.now());
    }

    private Map<String, Object> startBody() {
        return Map.of(
                "standardId", STD.toString(),
                "tenantProfile", Map.of(
                        "organizationName", "ACME", "industry", "manufacturing",
                        "size", "PME", "language", "fr",
                        "knownProcesses", List.of("achats")),
                "documentKeys", List.of("manuel-qualite"));
    }

    // ---- catalog / list / get ----

    @Test @WithMockUser
    void catalog_returns200() throws Exception {
        when(service.catalog()).thenReturn(List.of(new DossierDto.DocumentView(
                "manuel-qualite", NormDocKind.MANUAL, "Manuel",
                DossierDocStatus.EN_ATTENTE, null, null, null, 7)));
        mockMvc.perform(get("/api/v1/standards/doc-dossiers/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("manuel-qualite"));
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.list()).thenReturn(List.of(view(DossierStatus.GENERE)));
        mockMvc.perform(get("/api/v1/standards/doc-dossiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].standardCode").value("iso-9001"));
    }

    @Test @WithMockUser
    void get_returns200() throws Exception {
        when(service.get(ID)).thenReturn(view(DossierStatus.GENERE));
        mockMvc.perform(get("/api/v1/standards/doc-dossiers/{id}", ID))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(ID)).thenThrow(new DossierNotFoundException(ID));
        mockMvc.perform(get("/api/v1/standards/doc-dossiers/{id}", ID))
                .andExpect(status().isNotFound());
    }

    // ---- start ----

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void start_returns201() throws Exception {
        when(service.start(any())).thenReturn(view(DossierStatus.GENERE));
        mockMvc.perform(post("/api/v1/standards/doc-dossiers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(startBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("GENERE"))
                .andExpect(jsonPath("$.progressPercent").value(100));
    }

    @Test @WithMockUser(roles = "USER")
    void start_insufficientRole_403() throws Exception {
        mockMvc.perform(post("/api/v1/standards/doc-dossiers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(startBody())))
                .andExpect(status().isForbidden());
    }

    @Test
    void start_unauthenticated_401() throws Exception {
        mockMvc.perform(post("/api/v1/standards/doc-dossiers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(startBody())))
                .andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void start_invalidBody_400() throws Exception {
        mockMvc.perform(post("/api/v1/standards/doc-dossiers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void start_unknownStandard_404() throws Exception {
        when(service.start(any())).thenThrow(new StandardNotFoundException(STD));
        mockMvc.perform(post("/api/v1/standards/doc-dossiers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(startBody())))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void start_nullCollections_mappedToEmpty() throws Exception {
        when(service.start(any())).thenReturn(view(DossierStatus.GENERE));
        Map<String, Object> body = Map.of(
                "standardId", STD.toString(),
                "tenantProfile", Map.of("organizationName", "ACME"));
        mockMvc.perform(post("/api/v1/standards/doc-dossiers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    // ---- retry ----

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void retry_returns200() throws Exception {
        when(service.retryFailed(ID)).thenReturn(view(DossierStatus.GENERE));
        mockMvc.perform(post("/api/v1/standards/doc-dossiers/{id}/retry", ID).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void retry_invalidState_409() throws Exception {
        when(service.retryFailed(ID)).thenThrow(new DossierStateException("finalisé"));
        mockMvc.perform(post("/api/v1/standards/doc-dossiers/{id}/retry", ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    // ---- finalize ----

    @Test @WithMockUser(roles = "DIRECTOR_QUALITY")
    void finalize_returns200() throws Exception {
        when(service.finalizeDossier(eq(ID), any())).thenReturn(view(DossierStatus.FINALISE));
        mockMvc.perform(post("/api/v1/standards/doc-dossiers/{id}/finalize", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("signature", "sig", "notes", "ok"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALISE"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void finalize_managerForbidden_403() throws Exception {
        mockMvc.perform(post("/api/v1/standards/doc-dossiers/{id}/finalize", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("signature", "sig"))))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "DIRECTOR_QUALITY")
    void finalize_missingSignature_400() throws Exception {
        mockMvc.perform(post("/api/v1/standards/doc-dossiers/{id}/finalize", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("notes", "x"))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "DIRECTOR_QUALITY")
    void finalize_notApproved_409() throws Exception {
        when(service.finalizeDossier(eq(ID), any()))
                .thenThrow(new DossierStateException("approuvées"));
        mockMvc.perform(post("/api/v1/standards/doc-dossiers/{id}/finalize", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("signature", "sig"))))
                .andExpect(status().isConflict());
    }
}
