package com.openlab.qualitos.quality.stakeholders.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderDto;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderPorts;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderService;
import com.openlab.qualitos.quality.stakeholders.domain.ImpactLevel;
import com.openlab.qualitos.quality.stakeholders.domain.MandatoryNature;
import com.openlab.qualitos.quality.stakeholders.domain.Relevance;
import com.openlab.qualitos.quality.stakeholders.domain.RequirementLink;
import com.openlab.qualitos.quality.stakeholders.domain.RequirementStatus;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderNotFoundException;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderSide;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderValidationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La surface HTTP du registre : lire est ouvert, écrire est un acte de
 * pilotage, et les assistants IA ne sont que des lectures.
 */
@Tag("web")
@WebMvcTest(controllers = StakeholderController.class)
@Import(MethodSecurityTestConfig.class)
class StakeholderControllerTest {

    static final String BASE = "/api/v1/stakeholders";
    static final UUID ID = UUID.randomUUID();
    static final UUID RID = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();
    static final LocalDate DEMAIN = LocalDate.now().plusDays(1);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @MockitoBean StakeholderService service;

    private static StakeholderDto.Sheet fiche() {
        return new StakeholderDto.Sheet(ID, "Client A", StakeholderCategory.CUSTOMERS,
                StakeholderSide.EXTERNAL, null, null, null, null, null, Relevance.TO_DETERMINE,
                null, null, ImpactLevel.LOW, ImpactLevel.HIGH, ImpactLevel.HIGH, null,
                null, null, null, null, null, null, false, List.of());
    }

    private static StakeholderDto.Requirement exigence() {
        return new StakeholderDto.Requirement(RID, null, "Respecter le taux de service", null,
                MandatoryNature.CONTRACTUAL, null, ImpactLevel.MEDIUM, null, null, null, null,
                RequirementStatus.APPLICABLE, null, null, null, null, null);
    }

    // ---------- lecture ----------

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitLeRegistre() throws Exception {
        when(service.list(any(), any())).thenReturn(List.of(
                new StakeholderDto.Row(ID, "Client A", StakeholderCategory.CUSTOMERS,
                        StakeholderSide.EXTERNAL, ImpactLevel.LOW, ImpactLevel.HIGH, ImpactLevel.HIGH,
                        Relevance.RELEVANT, null, null, false, 2)));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Client A"))
                .andExpect(jsonPath("$[0].criticality").value("HIGH"))
                .andExpect(jsonPath("$[0].requirementCount").value(2));
    }

    @Test
    @WithMockUser(roles = "USER")
    void lesFiltresSontTransmisTelsQuels() throws Exception {
        when(service.list(any(), any())).thenReturn(List.of());

        mockMvc.perform(get(BASE).param("side", "INTERNAL").param("search", "client"))
                .andExpect(status().isOk());

        verify(service).list(StakeholderSide.INTERNAL, "client");
    }

    @Test
    @WithMockUser(roles = "USER")
    void lesTuilesEtLaFicheSeLisent() throws Exception {
        when(service.statistics()).thenReturn(
                new StakeholderDto.Statistics(5, 2, 3, 1, 1, 2, 3));
        when(service.get(ID)).thenReturn(fiche());

        mockMvc.perform(get(BASE + "/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.critical").value(1))
                .andExpect(jsonPath("$.requirementsToReassess").value(3));
        mockMvc.perform(get(BASE + "/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Client A"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void uneFicheInconnueRend404() throws Exception {
        when(service.get(ID)).thenThrow(new StakeholderNotFoundException(ID));

        mockMvc.perform(get(BASE + "/" + ID)).andExpect(status().isNotFound());
    }

    // ---------- écriture : un acte de pilotage ----------

    @Test
    @WithMockUser(roles = "USER")
    void unSimpleUtilisateurNEcritPas() throws Exception {
        // Déterminer les parties intéressées pertinentes, c'est fixer le
        // périmètre du système de management : ce n'est pas à tout le monde.
        String corps = json.writeValueAsString(new StakeholderWebDto.CreateRequest(
                "Client A", StakeholderCategory.CUSTOMERS, StakeholderSide.EXTERNAL));

        mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/" + ID).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/" + ID + "/review").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void lePilotageInscritUnePartieInteressee() throws Exception {
        when(service.create(anyString(), any(), any())).thenReturn(fiche());
        String corps = json.writeValueAsString(new StakeholderWebDto.CreateRequest(
                "Client A", StakeholderCategory.CUSTOMERS, StakeholderSide.EXTERNAL));

        mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));

        verify(service).create("Client A", StakeholderCategory.CUSTOMERS, StakeholderSide.EXTERNAL);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneInscriptionMalFormeeEstRefuseeAvantLeService() throws Exception {
        for (String corps : List.of(
                "{\"name\":\"\",\"category\":\"CUSTOMERS\",\"side\":\"EXTERNAL\"}",
                "{\"name\":\"Client A\",\"category\":null,\"side\":\"EXTERNAL\"}",
                "{\"name\":\"Client A\",\"category\":\"CUSTOMERS\"}")) {
            mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(corps))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneDecisionNonJustifieeRend400() throws Exception {
        when(service.update(eq(ID), any())).thenThrow(
                new StakeholderValidationException("A relevance decision must be justified"));

        mockMvc.perform(put(BASE + "/" + ID).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relevance\":\"RELEVANT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void laMatriceCoteEnUneEcriture() throws Exception {
        when(service.rate(any())).thenReturn(List.of());
        String corps = "{\"ratings\":{\"" + ID
                + "\":{\"qualityImpact\":\"HIGH\",\"regulatoryImpact\":\"LOW\"}}}";

        mockMvc.perform(put(BASE + "/ratings").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isOk());

        verify(service).rate(any());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneMatriceVideEstRefuseeAvantLeService() throws Exception {
        mockMvc.perform(put(BASE + "/ratings").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ratings\":{}}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // ---------- exigences ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneExigenceSAjouteSeSuitEtSeRetire() throws Exception {
        when(service.addRequirement(eq(ID), any())).thenReturn(exigence());
        when(service.setRequirementStatus(ID, RID, RequirementStatus.CONTROLLED)).thenReturn(exigence());
        when(service.linkRequirement(eq(ID), eq(RID), eq(RequirementLink.AUDIT), any()))
                .thenReturn(exigence());

        mockMvc.perform(post(BASE + "/" + ID + "/requirements").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirement\":\"Respecter le taux de service\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requirement").value("Respecter le taux de service"));

        mockMvc.perform(put(BASE + "/" + ID + "/requirements/" + RID + "/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONTROLLED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put(BASE + "/" + ID + "/requirements/" + RID + "/link").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"AUDIT\",\"target\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete(BASE + "/" + ID + "/requirements/" + RID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).removeRequirement(ID, RID);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneExigenceSansLibelleEstRefuseeAvantLeService() throws Exception {
        mockMvc.perform(post(BASE + "/" + ID + "/requirements").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requirement\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneCapaSOuvrePourUneExigenceNonTenue() throws Exception {
        when(service.openCapa(eq(ID), eq(RID), anyString(), eq(OWNER), any(), eq(DEMAIN)))
                .thenReturn(exigence());
        String corps = json.writeValueAsString(new StakeholderWebDto.OpenCapaRequest(
                "Remettre le taux de service", OWNER, "Alice Martin", DEMAIN));

        mockMvc.perform(post(BASE + "/" + ID + "/requirements/" + RID + "/capa").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated());

        verify(service).openCapa(ID, RID, "Remettre le taux de service", OWNER, "Alice Martin", DEMAIN);
    }

    // ---------- assistants IA ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void lesTroisAssistantsProposentSansRienInscrire() throws Exception {
        when(service.suggest(anyString(), any())).thenReturn(List.of(
                new StakeholderPorts.SuggestedStakeholder("Donneurs d'ordre",
                        StakeholderCategory.CUSTOMERS, "40 % du CA")));
        when(service.extract(any(), anyString())).thenReturn(List.of(
                new StakeholderPorts.ExtractedRequirement("citation", "art. 7.2",
                        "Fournir un certificat", MandatoryNature.CONTRACTUAL, "Contrôle à réception")));
        when(service.changes(any(), anyString(), anyString())).thenReturn(List.of(
                new StakeholderPorts.DetectedChange("Taux porté à 98 %", "Art. 9.1", "Avenant 2",
                        ImpactLevel.HIGH)));

        mockMvc.perform(post(BASE + "/assistant/suggestions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sector\":\"Aéronautique\",\"characteristics\":\"PME\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("CUSTOMERS"));

        mockMvc.perform(post(BASE + "/assistant/requirements").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentName\":\"c.pdf\",\"documentText\":\"Le texte du contrat.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quote").value("citation"));

        mockMvc.perform(post(BASE + "/assistant/changes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"previousText\":\"avant\",\"currentText\":\"après\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].impact").value("HIGH"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void lesAssistantsNeSontPasOuvertsATous() throws Exception {
        mockMvc.perform(post(BASE + "/assistant/suggestions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sector\":\"X\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneDemandeDAssistantSansTexteEstRefuseeAvantLeService() throws Exception {
        for (String[] appel : new String[][] {
                {"/assistant/suggestions", "{\"sector\":\"  \"}"},
                {"/assistant/requirements", "{\"documentText\":\"\"}"},
                {"/assistant/changes", "{\"previousText\":\"avant\"}"}}) {
            mockMvc.perform(post(BASE + appel[0]).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(appel[1]))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void lHumainDesigneCeQueLeChangementRemetEnCause() throws Exception {
        when(service.markToReassess(eq(ID), anyList())).thenReturn(List.of(exigence()));

        mockMvc.perform(post(BASE + "/" + ID + "/requirements/reassess").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirementIds\":[\"" + RID + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(RID.toString()));

        verify(service).markToReassess(ID, List.of(RID));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneReevaluationSansExigenceEstRefuseeAvantLeService() throws Exception {
        mockMvc.perform(post(BASE + "/" + ID + "/requirements/reassess").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"requirementIds\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
