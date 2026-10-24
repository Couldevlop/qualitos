package com.openlab.qualitos.quality.smi.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.smi.application.SmiDto;
import com.openlab.qualitos.quality.smi.application.SmiStandardsService;
import com.openlab.qualitos.quality.smi.domain.StandardUnavailableException;
import com.openlab.qualitos.quality.standards.AdoptionConflictException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ajouter une norme au SMI est un paramétrage : l'administrateur du client, et
 * lui seul. Les autres rôles — manager et directeur qualité, auditeur,
 * utilisateur, et même le super administrateur de l'éditeur — reçoivent 403,
 * avant toute lecture du corps (ADR 0065).
 */
@Tag("web")
@WebMvcTest(controllers = SmiStandardsController.class)
@Import(MethodSecurityTestConfig.class)
class SmiStandardsControllerTest {

    static final UUID ISO_14001 = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired MockMvc mockMvc;
    @MockitoBean SmiStandardsService service;

    static String corps(UUID id) {
        return "{\"standardId\":\"" + id + "\"}";
    }

    // ---------- l'administrateur du client ----------

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurDuClientVoitLesNormesAAjouter() throws Exception {
        when(service.candidates()).thenReturn(List.of(new SmiDto.Candidate(ISO_14001, "iso-14001",
                "ISO 14001:2015 — Systèmes de management environnemental", "2015", true, List.of("iso-9001"))));

        mockMvc.perform(get("/api/v1/smi/standards/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("iso-14001"))
                .andExpect(jsonPath("$[0].suggested").value(true))
                .andExpect(jsonPath("$[0].sharedWith[0]").value("iso-9001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurDuClientAjouteUneNorme() throws Exception {
        UUID adoption = UUID.randomUUID();
        when(service.add(ISO_14001)).thenReturn(new SmiDto.StandardRef(adoption, "iso-14001", "ISO 14001:2015"));

        mockMvc.perform(post("/api/v1/smi/standards").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps(ISO_14001)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adoptionId").value(adoption.toString()))
                .andExpect(jsonPath("$.code").value("iso-14001"));
    }

    /** Les noms que le rôle a portés (ADR 0020) valent le rôle. */
    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TENANT_ADMIN"})
    void lesAliasDuRoleAdministrateurSontAdmis(String alias) throws Exception {
        when(service.candidates()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/smi/standards/candidates").with(user("admin").roles(alias)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void unCorpsInvalideResteUnQuatreCentsPourQuiALeDroit() throws Exception {
        mockMvc.perform(post("/api/v1/smi/standards").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void uneNormeDejaAdopteeRend409EtUneNormeHorsSystemeDeManagement404() throws Exception {
        UUID iatf = UUID.randomUUID();
        when(service.add(ISO_14001)).thenThrow(
                new StandardUnavailableException(ISO_14001, StandardUnavailableException.Reason.ALREADY_ADOPTED));
        when(service.add(iatf)).thenThrow(
                new StandardUnavailableException(iatf, StandardUnavailableException.Reason.NOT_A_MANAGEMENT_SYSTEM));

        mockMvc.perform(post("/api/v1/smi/standards").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps(ISO_14001)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/smi-standard-already-adopted"));
        mockMvc.perform(post("/api/v1/smi/standards").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps(iatf)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/smi-standard-not-management-system"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void uneAdoptionConcurrenteResteUnConflit() throws Exception {
        when(service.add(ISO_14001)).thenThrow(new AdoptionConflictException("already adopted"));

        mockMvc.perform(post("/api/v1/smi/standards").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps(ISO_14001)))
                .andExpect(status().isConflict());
    }

    // ---------- tous les autres ----------

    @ParameterizedTest
    @ValueSource(strings = {"QUALITY_MANAGER", "QUALITY_DIRECTOR", "DIRECTOR_QUALITY", "AUDITOR", "USER",
            "EXTERNAL_AUDITOR", "SUPER_ADMIN"})
    void lesAutresRolesSontRefusesAvantLaLectureDuCorps(String role) throws Exception {
        mockMvc.perform(get("/api/v1/smi/standards/candidates").with(user("u").roles(role)))
                .andExpect(status().isForbidden());
        // Corps malformé : la réponse reste 403, le refus ne dépend pas de ce qu'on envoie.
        mockMvc.perform(post("/api/v1/smi/standards").with(user("u").roles(role)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"standardId\":"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/smi/standards").with(user("u").roles(role)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps(ISO_14001)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithAnonymousUser
    void anonymeRefuse() throws Exception {
        mockMvc.perform(get("/api/v1/smi/standards/candidates")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/smi/standards").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps(ISO_14001)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
