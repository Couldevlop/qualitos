package com.openlab.qualitos.quality.riskregister.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterDto;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterService;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityActionStatus;
import com.openlab.qualitos.quality.riskregister.domain.RegisterNotFoundException;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRequirement;
import com.openlab.qualitos.quality.riskregister.domain.RegisterType;
import com.openlab.qualitos.quality.riskregister.domain.RegisterValidationException;
import com.openlab.qualitos.quality.riskregister.domain.RiskCapaKind;
import com.openlab.qualitos.quality.riskregister.domain.RiskDecision;
import com.openlab.qualitos.quality.riskregister.domain.RiskLevel;
import com.openlab.qualitos.quality.riskregister.domain.RiskStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
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
 * Le partage des rôles sur le registre : tout authentifié lit, seuls les
 * profils de pilotage écrivent ; le corps ne porte ni tenant ni référence.
 */
@Tag("web")
@WebMvcTest(controllers = RiskRegisterController.class)
@Import(MethodSecurityTestConfig.class)
class RiskRegisterControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RiskRegisterService service;

    static final UUID RISQUE = UUID.randomUUID();
    static final UUID OPP = UUID.randomUUID();
    static final UUID ACTION = UUID.randomUUID();
    static final String BASE = "/api/v1/risk-register";

    static final String CORPS_RISQUE = "{\"title\":\"Dérive soudure\",\"type\":\"QUALITY\","
            + "\"process\":\"Production\",\"owner\":\"M. Kone\",\"origin\":\"FMEA\","
            + "\"grossSeverity\":4,\"grossProbability\":3,\"decision\":\"REDUCE\","
            + "\"requirements\":[\"ISO_9001_6_1\",\"IATF_16949_6_1_2\"]}";
    static final String CORPS_CAPA = "{\"title\":\"Carte SPC\",\"kind\":\"PREVENTIVE\","
            + "\"assignee\":\"A. Diallo\",\"dueDate\":\"2026-12-01\"}";

    static final String CORPS_OPP = "{\"title\":\"Automatiser SPC\",\"type\":\"QUALITY\","
            + "\"process\":\"Production\",\"owner\":\"Mme Diallo\",\"gain\":4,\"feasibility\":4}";

    static RiskRegisterDto.RiskView vue() {
        return new RiskRegisterDto.RiskView(RISQUE, "R-014", "Dérive soudure", RegisterType.QUALITY, "Production",
                null, "M. Kone", null, null, RegisterOrigin.FMEA, "PFMEA-7 #3", null, 4, 3, 12, RiskLevel.HIGH,
                4, 2, 8, RiskLevel.MEDIUM, RiskDecision.REDUCE, RiskStatus.IN_TREATMENT,
                List.of(RegisterRequirement.ISO_9001_6_1), null, null, Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitLeRegistreEtLaFiche() throws Exception {
        when(service.risks()).thenReturn(List.of(vue()));
        when(service.risk(RISQUE)).thenReturn(new RiskRegisterDto.RiskSheet(vue(),
                List.of(new RiskRegisterDto.CapaView(UUID.randomUUID(), "Carte SPC", LocalDate.of(2026, 11, 1),
                        "IN_PROGRESS", RiskCapaKind.PREVENTIVE, "A. Diallo")), List.of()));
        when(service.opportunities()).thenReturn(List.of());
        when(service.suggestions()).thenReturn(new RiskRegisterDto.Suggestions(List.of("Production"),
                List.of(), List.of()));

        mockMvc.perform(get(BASE + "/risks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reference").value("R-014"))
                .andExpect(jsonPath("$[0].grossLevel").value("HIGH"))
                .andExpect(jsonPath("$[0].residualScore").value(8));
        mockMvc.perform(get(BASE + "/risks/" + RISQUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capas[0].status").value("IN_PROGRESS"));
        mockMvc.perform(get(BASE + "/opportunities")).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processes[0]").value("Production"));
    }

    @Test
    @WithAnonymousUser
    void unAnonymeNeLitRien() throws Exception {
        mockMvc.perform(get(BASE + "/risks")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"QUALITY_MANAGER", "DIRECTOR_QUALITY", "ADMIN_TENANT", "SUPER_ADMIN"})
    void lesProfilsDePilotageCreentUnRisque(String role) throws Exception {
        when(service.createRisk(any())).thenReturn(vue());

        mockMvc.perform(post(BASE + "/risks").with(csrf()).with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS_RISQUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("R-014"));
    }

    @Test
    @WithMockUser(roles = {"USER", "AUDITOR"})
    void niUnUtilisateurNiUnAuditeurNEcrivent() throws Exception {
        mockMvc.perform(post(BASE + "/risks").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(CORPS_RISQUE)).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/risks/" + RISQUE).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(CORPS_RISQUE)).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/risks/" + RISQUE + "/capa").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(CORPS_CAPA)).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/opportunities").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(CORPS_OPP)).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/opportunities/" + OPP).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(CORPS_OPP)).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/opportunities/" + OPP + "/actions").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}")).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/opportunities/" + OPP + "/actions/" + ACTION).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}")).andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/opportunities/" + OPP + "/actions/" + ACTION).with(csrf()))
                .andExpect(status().isForbidden());
        // Le brouillon expose le texte de l'objet source : réservé à qui peut créer le risque.
        mockMvc.perform(get(BASE + "/sources/FMEA/" + UUID.randomUUID())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void leCorpsEstTransmisSansTenantNiReference() throws Exception {
        when(service.createRisk(any())).thenReturn(vue());
        String avecIntrus = CORPS_RISQUE.replace("{", "{\"tenantId\":\"" + UUID.randomUUID()
                + "\",\"reference\":\"R-999\",");

        mockMvc.perform(post(BASE + "/risks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(avecIntrus))
                .andExpect(status().isCreated());
        verify(service).createRisk(argThat(c -> c.title().equals("Dérive soudure")
                && c.grossSeverity() == 4 && c.requirements().size() == 2
                && c.origin() == RegisterOrigin.FMEA && c.decision() == RiskDecision.REDUCE));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneNoteHorsBornesOuUnChampManquantRend400AvantLeService() throws Exception {
        mockMvc.perform(post(BASE + "/risks").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(CORPS_RISQUE.replace("\"grossSeverity\":4", "\"grossSeverity\":6")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/risks").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(CORPS_RISQUE.replace("\"owner\":\"M. Kone\",", "")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/risks").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(CORPS_RISQUE.replace("QUALITY", "INCONNU")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/opportunities").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(CORPS_OPP.replace("\"gain\":4", "\"gain\":0")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneRegleDuDomaineRend422AvecLeChamp() throws Exception {
        when(service.reviseRisk(eq(RISQUE), any())).thenThrow(new RegisterValidationException(
                "residualSeverity", "La cotation résiduelle visée ne peut pas dépasser la cotation brute."));

        mockMvc.perform(put(BASE + "/risks/" + RISQUE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS_RISQUE))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("residualSeverity"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneFicheIntrouvableRend404() throws Exception {
        when(service.risk(RISQUE)).thenThrow(new RegisterNotFoundException("Risk", RISQUE));
        doThrow(new RegisterNotFoundException("Action", ACTION)).when(service).deleteAction(OPP, ACTION);

        mockMvc.perform(get(BASE + "/risks/" + RISQUE)).andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/opportunities/" + OPP + "/actions/" + ACTION).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void ouvrirUneCapaEtGererLesActions() throws Exception {
        when(service.openCapa(eq(RISQUE), any())).thenReturn(new RiskRegisterDto.CapaView(UUID.randomUUID(),
                "Carte SPC", LocalDate.of(2026, 12, 1), "OPEN", RiskCapaKind.CORRECTIVE, "A. Diallo"));
        when(service.addAction(eq(OPP), any())).thenReturn(new RiskRegisterDto.ActionView(ACTION, 3, "Chiffrer",
                null, OpportunityActionStatus.TO_START));
        when(service.reviseAction(eq(OPP), eq(ACTION), any())).thenReturn(new RiskRegisterDto.ActionView(ACTION, 3,
                "Chiffrer", null, OpportunityActionStatus.DONE));

        mockMvc.perform(post(BASE + "/risks/" + RISQUE + "/capa").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS_CAPA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.kind").value("CORRECTIVE"))
                .andExpect(jsonPath("$.assignee").value("A. Diallo"));
        // Nature, responsable et échéance sont exigés dès la frontière HTTP.
        mockMvc.perform(post(BASE + "/risks/" + RISQUE + "/capa").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Carte SPC\",\"dueDate\":\"2026-12-01\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/risks/" + RISQUE + "/capa").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE + "/opportunities/" + OPP + "/actions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Chiffrer\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(3));
        mockMvc.perform(put(BASE + "/opportunities/" + OPP + "/actions/" + ACTION).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Chiffrer\",\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
        mockMvc.perform(delete(BASE + "/opportunities/" + OPP + "/actions/" + ACTION).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).deleteAction(OPP, ACTION);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void leBrouillonDUneSourceEtUneOrigineInconnue() throws Exception {
        UUID source = UUID.randomUUID();
        when(service.draft(RegisterOrigin.FMEA, source)).thenReturn(new RiskRegisterDto.RiskDraft(
                RegisterOrigin.FMEA, source, "PFMEA-7 #3", "Cordon poreux", null, null, 4, 3, null, true, null,
                List.of()));

        mockMvc.perform(get(BASE + "/sources/FMEA/" + source))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originRef").value("PFMEA-7 #3"))
                .andExpect(jsonPath("$.eligible").value(true));
        mockMvc.perform(get(BASE + "/sources/PIRATE/" + source)).andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/sources/FMEA/pas-un-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void opportunitesCreeesEtRevisees() throws Exception {
        RiskRegisterDto.OpportunityView o = new RiskRegisterDto.OpportunityView(OPP, "O-003", "Automatiser SPC",
                RegisterType.QUALITY, "Production", null, "Mme Diallo", null, null, null, RegisterOrigin.DIRECT,
                null, 4, 4, 16, com.openlab.qualitos.quality.riskregister.domain.OpportunityLevel.PRIORITY,
                com.openlab.qualitos.quality.riskregister.domain.OpportunityDecision.UNDECIDED,
                com.openlab.qualitos.quality.riskregister.domain.OpportunityStatus.UNDER_STUDY, List.of(), null,
                Instant.EPOCH, Instant.EPOCH);
        when(service.createOpportunity(any())).thenReturn(o);
        when(service.reviseOpportunity(eq(OPP), any())).thenReturn(o);
        when(service.opportunity(OPP)).thenReturn(new RiskRegisterDto.OpportunitySheet(o, List.of(), List.of()));

        mockMvc.perform(post(BASE + "/opportunities").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS_OPP))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.level").value("PRIORITY"));
        mockMvc.perform(put(BASE + "/opportunities/" + OPP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS_OPP))
                .andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/opportunities/" + OPP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opportunity.reference").value("O-003"));
    }
}
