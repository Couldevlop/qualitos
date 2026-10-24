package com.openlab.qualitos.quality.spc.gagerr.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.spc.capability.domain.SpcNotFoundException;
import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;
import com.openlab.qualitos.quality.spc.gagerr.application.GageRrStudyDto;
import com.openlab.qualitos.quality.spc.gagerr.application.GageRrStudyService;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrDecision;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrOperator;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrStudy;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrFixtures;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le partage des rôles sur les études R&amp;R : tout authentifié lit, l'action
 * {@code spc.manage} écrit ; le corps ne porte ni tenant, ni %GRR, ni verdict ;
 * une requête mal formée est refusée avant le service.
 */
@Tag("web")
@WebMvcTest(controllers = GageRrStudyController.class)
@Import(MethodSecurityTestConfig.class)
class GageRrStudyControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean GageRrStudyService service;

    static final UUID ETUDE = UUID.randomUUID();
    static final String BASE = "/api/v1/spc/gage-rr";

    /** Le corps de la grille du classeur. */
    static String corps() {
        List<String> blocs = new ArrayList<>();
        for (List<List<Double>> bloc : GageRrFixtures.workbookGrid()) {
            List<String> lignes = new ArrayList<>();
            for (List<Double> essais : bloc) {
                lignes.add(essais.toString());
            }
            blocs.add("[" + String.join(",", lignes) + "]");
        }
        return "{\"partReference\":\"Arbre AX-210\",\"characteristic\":\"Diamètre extérieur (mm)\","
                + "\"instrument\":\"Micromètre\",\"lsl\":9.9,\"usl\":10.1,"
                + "\"operatorCount\":3,\"partCount\":10,\"trialCount\":3,\"sigmaMultiplier\":5.15,"
                + "\"studyDate\":\"2026-10-19\",\"responsibleUserId\":\"6f1c2a4e-1f0b-4c47-9d0e-2b8f3a6c7d11\","
                + "\"responsibleName\":\"Jeanne Leroy\",\"operators\":["
                + "{\"accountId\":\"0b6f5c1e-1a2b-4c3d-8e9f-000000000001\",\"name\":\"Alice Martin\"},"
                + "{\"accountId\":\"0b6f5c1e-1a2b-4c3d-8e9f-000000000002\",\"name\":\"Bruno Petit\"},"
                + "{\"accountId\":\"0b6f5c1e-1a2b-4c3d-8e9f-000000000003\",\"name\":\"Chloé Durand\"}],"
                + "\"measurements\":[" + String.join(",", blocs) + "]}";
    }

    /** La vraie fiche de l'exemple du classeur, telle que le service la rend. */
    static GageRrStudyDto.StudySheet fiche() {
        GageRrStudy e = GageRrStudy.record(UUID.randomUUID(), GageRrFixtures.workbookDetails(), null, Instant.EPOCH);
        GageRrStudy avecId = new GageRrStudy(ETUDE, e.getTenantId(), e.getPartReference(), e.getCharacteristic(),
                e.getInstrument(), e.getLsl(), e.getUsl(), e.getOperatorCount(), e.getPartCount(),
                e.getTrialCount(), e.getSigmaMultiplier(), e.getStudyDate(), e.getResponsibleUserId(),
                e.getResponsibleName(), e.getOperators(), e.getMeasurements(), e.getGrrPercent(),
                e.getGrrTolerancePercent(), e.getNdc(), e.getDecision(), e.getAnovaGrrPercent(), e.getAnovaNdc(),
                null, Instant.EPOCH, Instant.EPOCH);
        return GageRrStudyService.sheetOf(avecId);
    }

    static GageRrStudyDto.StudySummary ligne() {
        return new GageRrStudyDto.StudySummary(ETUDE, "Arbre AX-210", "Diamètre extérieur (mm)", "Micromètre",
                LocalDate.of(2026, 10, 19), null, "Jeanne Leroy",
                List.of(new GageRrOperator(GageRrFixtures.OPERATEUR_A, "Alice Martin")), 3, 10, 3, 5.15,
                21.0, 24.74, 6, GageRrDecision.CONDITIONAL, 21.16, 6, Instant.EPOCH);
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitLeRegistreEtLaFiche() throws Exception {
        when(service.list()).thenReturn(List.of(ligne()));
        when(service.get(ETUDE)).thenReturn(fiche());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partReference").value("Arbre AX-210"))
                .andExpect(jsonPath("$[0].decision").value("CONDITIONAL"))
                .andExpect(jsonPath("$[0].grrPercent").value(21.0))
                .andExpect(jsonPath("$[0].operators[0].name").value("Alice Martin"));
        mockMvc.perform(get(BASE + "/" + ETUDE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.study.id").value(ETUDE.toString()))
                .andExpect(jsonPath("$.calculation.ndc").value(6))
                .andExpect(jsonPath("$.calculation.decision").value("CONDITIONAL"))
                .andExpect(jsonPath("$.calculation.grr.percentOfTotal").isNumber())
                .andExpect(jsonPath("$.calculation.anova.interactionPooled").value(true))
                .andExpect(jsonPath("$.calculation.anova.fullTable[2].source").value("INTERACTION"))
                .andExpect(jsonPath("$.calculation.anova.fullTable[4].meanSquare").doesNotExist())
                .andExpect(jsonPath("$.measurements[0][0][0]").value(9.955));
    }

    @Test
    @WithAnonymousUser
    void unAnonymeNeLitRien() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"QUALITY_MANAGER", "QUALITY_DIRECTOR", "ADMIN_TENANT", "SUPER_ADMIN"})
    void lesProfilsDePilotageEcrivent(String role) throws Exception {
        when(service.create(any())).thenReturn(fiche());
        when(service.revise(eq(ETUDE), any())).thenReturn(fiche());

        mockMvc.perform(post(BASE).with(csrf()).with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.study.grrPercent").value(21.0));
        mockMvc.perform(put(BASE + "/" + ETUDE).with(csrf()).with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(corps()))
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

        verify(service).create(argThat(c -> c.partReference().equals("Arbre AX-210")
                && c.instrument().equals("Micromètre") && c.characteristic().startsWith("Diamètre")
                && c.lsl() == 9.9 && c.usl() == 10.1 && c.operatorCount() == 3 && c.partCount() == 10
                && c.trialCount() == 3 && c.sigmaMultiplier() == 5.15
                && c.studyDate().equals(LocalDate.of(2026, 10, 19))
                && c.responsibleUserId().equals(GageRrFixtures.RESPONSABLE)
                && c.responsibleName().equals("Jeanne Leroy")
                && c.operators().size() == 3 && c.operators().get(2).name().equals("Chloé Durand")
                && c.operators().get(0).accountId().equals(GageRrFixtures.OPERATEUR_A)
                && c.measurements().size() == 3 && c.measurements().get(0).size() == 10
                && c.measurements().get(2).get(9).get(2) == 10.077));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unTenantDansLeCorpsEstIgnore() throws Exception {
        when(service.create(any())).thenReturn(fiche());
        String avecTenant = corps().replaceFirst("\\{", "{\"tenantId\":\"" + UUID.randomUUID() + "\",\"grrPercent\":1,");

        mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(avecTenant))
                .andExpect(status().isCreated());
        verify(service).create(argThat(c -> c.partReference().equals("Arbre AX-210")));
    }

    @Test
    @WithMockUser(roles = {"USER", "AUDITOR"})
    void niUnUtilisateurNiUnAuditeurNEcrivent() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(corps())).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/" + ETUDE).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(corps())).andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/" + ETUDE).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneRequeteMalFormeeEstRefuseeAvantLeService() throws Exception {
        List<String> corpsFautifs = List.of(
                corps().replace("\"partReference\":\"Arbre AX-210\"", "\"partReference\":\"\""),
                corps().replace("\"operatorCount\":3", "\"operatorCount\":4"),
                corps().replace("\"partCount\":10", "\"partCount\":11"),
                corps().replace("\"trialCount\":3", "\"trialCount\":1"),
                corps().replace("\"sigmaMultiplier\":5.15,", ""),
                corps().replace("\"studyDate\":\"2026-10-19\"", "\"studyDate\":null"),
                corps().replace("\"lsl\":9.9,", ""),
                corps().replace("{\"accountId\":\"0b6f5c1e-1a2b-4c3d-8e9f-000000000001\",\"name\":\"Alice Martin\"}",
                        "{\"accountId\":null,\"name\":\"Alice Martin\"}"),
                corps().replace("\"name\":\"Bruno Petit\"", "\"name\":\" \""),
                corps().replaceFirst("\\[9.955, 9.951, 9.94\\]", "[9.955]"),
                "{\"partReference\":\"x\"}",
                "pas du json");
        for (String c : corpsFautifs) {
            mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(c))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneEtudeIncoherenteRend422AvecLeChamp() throws Exception {
        when(service.create(any())).thenThrow(new SpcValidationException("operators", "doublon"));

        mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corps()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("operators"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneEtudeIntrouvableOuDUnAutreClientRend404() throws Exception {
        when(service.get(ETUDE)).thenThrow(new SpcNotFoundException(ETUDE));
        when(service.revise(eq(ETUDE), any())).thenThrow(new SpcNotFoundException(ETUDE));
        doThrow(new SpcNotFoundException(ETUDE)).when(service).delete(ETUDE);

        mockMvc.perform(get(BASE + "/" + ETUDE)).andExpect(status().isNotFound());
        mockMvc.perform(put(BASE + "/" + ETUDE).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(corps())).andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/" + ETUDE).with(csrf())).andExpect(status().isNotFound());
    }
}
