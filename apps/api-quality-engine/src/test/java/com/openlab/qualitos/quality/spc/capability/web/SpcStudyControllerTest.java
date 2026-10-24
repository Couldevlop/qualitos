package com.openlab.qualitos.quality.spc.capability.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.spc.capability.application.SpcStudyDto;
import com.openlab.qualitos.quality.spc.capability.application.SpcStudyService;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStatus;
import com.openlab.qualitos.quality.spc.capability.domain.SpcNotFoundException;
import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le partage des rôles sur les études SPC : tout authentifié lit, l'action
 * {@code spc.manage} écrit ; le corps ne porte ni tenant, ni Cp, ni Cpk.
 */
@Tag("web")
@WebMvcTest(controllers = {SpcStudyController.class, SpcSettingsController.class})
@Import(MethodSecurityTestConfig.class)
class SpcStudyControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SpcStudyService service;

    static final UUID ETUDE = UUID.randomUUID();
    static final String BASE = "/api/v1/spc/studies";

    static String corps() {
        List<String> lignes = new ArrayList<>();
        for (int r = 0; r < 5; r++) {
            lignes.add("[" + String.join(",", Collections.nCopies(20, "500")) + "]");
        }
        return "{\"partReference\":\"Shaft AX\",\"characteristic\":\"Weight\",\"specification\":\"500 ± 25 gram\","
                + "\"machine\":\"M-04\",\"leaderUserId\":\"6f1c2a4e-1f0b-4c47-9d0e-2b8f3a6c7d11\","
                + "\"leaderName\":\"J. Martin\",\"studyDate\":\"2026-09-14\",\"actionPlan\":false,"
                + "\"lsl\":475,\"usl\":525,\"subgroupSize\":5,\"measurements\":[" + String.join(",", lignes) + "]}";
    }

    static SpcStudyDto.StudySummary ligne() {
        return new SpcStudyDto.StudySummary(ETUDE, "Shaft AX", "Weight", "500 ± 25 gram", "M-04",
                null, "J. Martin", LocalDate.of(2026, 9, 14), false, 1.84, 1.769, CapabilityStatus.CAPABLE,
                new BigDecimal("1.67"), new BigDecimal("1.33"), Instant.EPOCH);
    }

    static SpcStudyDto.StudySheet fiche() {
        return new SpcStudyDto.StudySheet(ligne(), 475, 525, 5, List.of(),
                new SpcStudyDto.CalculationView(List.of(), List.of(), 20, 9980.6, 499.03, 10.55, 4.5279,
                        1.84, 1.912, 1.769, 1.769, 505.149, 492.911, 22.261, 0.0, 2.33, 0.58, 0, 2.11,
                        54.3, -0.97, 3.88));
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitLeRegistreLaFicheEtLesSeuils() throws Exception {
        when(service.list()).thenReturn(List.of(ligne()));
        when(service.get(ETUDE)).thenReturn(fiche());
        when(service.settings()).thenReturn(new SpcStudyDto.SettingsView(
                new BigDecimal("1.67"), new BigDecimal("1.33"), true));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partReference").value("Shaft AX"))
                .andExpect(jsonPath("$[0].status").value("CAPABLE"))
                .andExpect(jsonPath("$[0].cpk").value(1.769));
        mockMvc.perform(get(BASE + "/" + ETUDE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculation.cp").value(1.84))
                .andExpect(jsonPath("$.subgroupSize").value(5));
        mockMvc.perform(get("/api/v1/spc/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capableThreshold").value(1.67))
                .andExpect(jsonPath("$.defaults").value(true));
    }

    @Test
    @WithAnonymousUser
    void unAnonymeNeLitRien() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/spc/settings")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"QUALITY_MANAGER", "QUALITY_DIRECTOR", "ADMIN_TENANT", "SUPER_ADMIN"})
    void lesProfilsDePilotageEcrivent(String role) throws Exception {
        when(service.create(any())).thenReturn(fiche());
        when(service.revise(eq(ETUDE), any())).thenReturn(fiche());
        when(service.setActionPlan(ETUDE, true)).thenReturn(ligne());

        mockMvc.perform(post(BASE).with(csrf()).with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.study.cpk").value(1.769));
        mockMvc.perform(put(BASE + "/" + ETUDE).with(csrf()).with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isOk());
        mockMvc.perform(patch(BASE + "/" + ETUDE + "/action-plan").with(csrf()).with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actionPlan\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete(BASE + "/" + ETUDE).with(csrf()).with(user("u").roles(role)))
                .andExpect(status().isNoContent());
        verify(service).delete(ETUDE);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void laSaisieArriveTelleQuelleAuService() throws Exception {
        when(service.create(any())).thenReturn(fiche());

        mockMvc.perform(post(BASE).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(corps())).andExpect(status().isCreated());

        verify(service).create(argThat(c -> c.partReference().equals("Shaft AX")
                && c.leaderName().equals("J. Martin") && c.subgroupSize() == 5
                && c.measurements().size() == 5 && c.measurements().get(0).size() == 20
                && c.lsl() == 475 && c.usl() == 525 && !c.actionPlan()
                && c.studyDate().equals(LocalDate.of(2026, 9, 14)) && c.machine().equals("M-04")
                && c.characteristic().equals("Weight") && c.specification().equals("500 ± 25 gram")
                && c.leaderUserId() != null));
    }

    @Test
    @WithMockUser(roles = {"USER", "AUDITOR"})
    void niUnUtilisateurNiUnAuditeurNEcrivent() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(corps())).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/" + ETUDE).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(corps())).andExpect(status().isForbidden());
        mockMvc.perform(patch(BASE + "/" + ETUDE + "/action-plan").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"actionPlan\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/" + ETUDE).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void seulLAdministrateurDuClientRegleLesSeuils() throws Exception {
        mockMvc.perform(put("/api/v1/spc/settings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"capableThreshold\":1.67,\"watchThreshold\":1.33}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurRegleLesSeuils() throws Exception {
        when(service.updateSettings(any())).thenReturn(new SpcStudyDto.SettingsView(
                new BigDecimal("1.50"), new BigDecimal("1.00"), false));

        mockMvc.perform(put("/api/v1/spc/settings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capableThreshold\":1.50,\"watchThreshold\":1.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capableThreshold").value(1.5));
        verify(service).updateSettings(argThat(c -> c.capableThreshold().compareTo(new BigDecimal("1.5")) == 0
                && c.watchThreshold().compareTo(BigDecimal.ONE) == 0));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void desSeuilsHorsBornesSontRefusesAvantLeService() throws Exception {
        mockMvc.perform(put("/api/v1/spc/settings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"capableThreshold\":12,\"watchThreshold\":1.333}")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneGrilleMalFormeeEstRefuseeAvantLeService() throws Exception {
        String sansReference = corps().replace("\"partReference\":\"Shaft AX\"", "\"partReference\":\"\"");
        String ligneCourte = corps().replaceFirst("\\[500,", "[");
        String tailleHorsTable = corps().replace("\"subgroupSize\":5", "\"subgroupSize\":11");
        for (String c : List.of(sansReference, ligneCourte, tailleHorsTable)) {
            mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(c))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(patch(BASE + "/" + ETUDE + "/action-plan").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneEtudeIncoherenteRend422AvecLeChamp() throws Exception {
        when(service.create(any())).thenThrow(new SpcValidationException("usl", "USL sous LSL"));

        mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("usl"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneEtudeIntrouvableRend404() throws Exception {
        when(service.get(ETUDE)).thenThrow(new SpcNotFoundException(ETUDE));
        doThrow(new SpcNotFoundException(ETUDE)).when(service).delete(ETUDE);

        mockMvc.perform(get(BASE + "/" + ETUDE)).andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/" + ETUDE).with(csrf())).andExpect(status().isNotFound());
    }
}
