package com.openlab.qualitos.quality.apqp;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
 * Le partage des rôles sur le cycle APQP d'un projet.
 *
 * <p>LIRE est ouvert à tout utilisateur authentifié : le cycle décrit ce que
 * l'organisation attend à chaque phase, et on ne peut pas demander à quelqu'un
 * de livrer une AMDEC processus en lui cachant à quel moment elle est due.
 *
 * <p>ÉCRIRE ne l'est pas. Supprimer une phase emporte ses livrables, et
 * réorganiser le cycle change la méthode elle-même : ce sont des actes de
 * pilotage qualité, pas des gestes d'exécution.
 */
@Tag("web")
@WebMvcTest(controllers = ApqpController.class)
@Import(MethodSecurityTestConfig.class)
class ApqpControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ApqpService service;

    private static final UUID PROJET = UUID.randomUUID();
    private static final UUID PHASE = UUID.randomUUID();
    private static final UUID LIVRABLE = UUID.randomUUID();

    /** La racine du cycle : le projet est dans le CHEMIN, le client dans le jeton. */
    private static final String RACINE = "/api/v1/apqp/projects/" + PROJET + "/phases";

    private static final String PHASE_BODY = """
            {"title":"Conception du processus","purpose":"Traduire le produit en moyens",
             "question":"Sait-on le fabriquer ?"}""";

    private static ApqpDto.PhaseResponse phase() {
        return new ApqpDto.PhaseResponse(
                PHASE, 2, 2, "Conception du processus",
                "Traduire le produit en moyens", "Sait-on le fabriquer ?",
                List.of(livrable()));
    }

    /** Un livrable tel que le service le rend : les colonnes du classeur, et elles seules. */
    private static ApqpDto.DeliverableResponse livrable() {
        return new ApqpDto.DeliverableResponse(
                LIVRABLE, 1, "PFMEA",
                "AMDEC processus rattachée à l'AMDEC produit", true,
                null, null, ApqpDeliverableStatus.NOT_STARTED, 0,
                false, null, null, null, null, null, 0);
    }

    /** Le cycle tel que la lecture le rend : le projet, ses phases, l'état du dossier. */
    private static ApqpDto.CycleResponse cycle() {
        return new ApqpDto.CycleResponse(PROJET, "Programme X", ApqpProjectType.NPI,
                "Client A", List.of(phase()), 0, 1);
    }

    // ---------- lecture ----------

    @Test
    @WithMockUser(roles = "USER")
    void toutUtilisateurAuthentifieLitLeCycle() throws Exception {
        when(service.cycle(PROJET)).thenReturn(cycle());

        mockMvc.perform(get(RACINE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectName").value("Programme X"))
                .andExpect(jsonPath("$.phases[0].title").value("Conception du processus"))
                .andExpect(jsonPath("$.phases[0].level").value(2))
                .andExpect(jsonPath("$.phases[0].deliverables[0].label").value("PFMEA"))
                .andExpect(jsonPath("$.phases[0].deliverables[0].ppap").value(true))
                .andExpect(jsonPath("$.phases[0].deliverables[0].status").value("NOT_STARTED"))
                // Le compte voyage avec le cycle : la section PPAP et le schéma
                // lisent le même état.
                .andExpect(jsonPath("$.ppapTotal").value(1))
                .andExpect(jsonPath("$.ppapDone").value(0));
    }

    @Test
    @WithAnonymousUser
    void unVisiteurAnonymeNeLitRien() throws Exception {
        mockMvc.perform(get(RACINE))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // ---------- écriture : qui peut ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void leManagerQualiteAjouteUnePhase() throws Exception {
        when(service.creerPhase(eq(PROJET), any())).thenReturn(phase());

        mockMvc.perform(post(RACINE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PHASE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PHASE.toString()));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR_QUALITY")
    void leDirecteurQualiteAussi() throws Exception {
        when(service.modifierPhase(eq(PROJET), eq(PHASE), any())).thenReturn(phase());

        mockMvc.perform(put(RACINE + "/" + PHASE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PHASE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurDuClientAussi() throws Exception {
        when(service.ajouterLivrable(eq(PROJET), eq(PHASE), any())).thenReturn(phase());

        mockMvc.perform(post(RACINE + "/" + PHASE + "/deliverables")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Plan de surveillance\",\"ppap\":true,"
                                + "\"expectedArtifact\":\"Plan de surveillance de production\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliverables[0].position").value(1));
    }

    // ---------- écriture : qui ne peut pas ----------

    @Test
    @WithMockUser(roles = "USER")
    void unOperateurNAjoutePasDePhase() throws Exception {
        mockMvc.perform(post(RACINE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PHASE_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "USER")
    void niNEnSupprimeUne() throws Exception {
        // La suppression emporte les livrables de la phase : c'est l'écriture
        // dont le refus compte le plus.
        mockMvc.perform(delete(RACINE + "/" + PHASE).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void unAuditeurLitLeCycleMaisNeLeRemanie() throws Exception {
        mockMvc.perform(put(RACINE + "/order")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phaseIds\":[\"" + PHASE + "\"]}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "USER")
    void unCorpsMalformeSansLeRoleResteUnRefusEtNonUneErreurDeSaisie() throws Exception {
        // Défaut d'ORDRE : Spring MVC valide la charge utile avant l'advice qui
        // porte @PreAuthorize, ce qui ferait d'un point d'entrée fermé un oracle
        // de validation (OWASP A01). Le pré-contrôle de méthode corrige cela.
        mockMvc.perform(post(RACINE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    // ---------- validation de surface ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unePhaseSansIntituleEstRefuseeALaFrontiere() throws Exception {
        // Le V n'affiche que l'intitulé : une phase sans nom y devient un jalon
        // muet, impossible à désigner.
        mockMvc.perform(post(RACINE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unLivrableSansLibelleEstRefuseALaFrontiere() throws Exception {
        mockMvc.perform(post(RACINE + "/" + PHASE + "/deliverables")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unAvancementHorsBornesEstRefuseALaFrontiere() throws Exception {
        // 0 à 100, et rien d'autre : la colonne I du classeur est un pourcentage,
        // et la contrainte de la base le redit (V131).
        mockMvc.perform(put(RACINE + "/" + PHASE + "/deliverables/" + LIVRABLE + "/completion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":false,\"percentComplete\":140}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    // ---------- achèvement d'un livrable ----------

    @Test
    @WithMockUser(roles = "USER")
    void unOperateurNeCochePasUnLivrable() throws Exception {
        mockMvc.perform(put(RACINE + "/" + PHASE + "/deliverables/" + LIVRABLE + "/completion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true}"))
                .andExpect(status().isForbidden());

        // L'autorisation se décide AVANT la lecture du corps (ADR 0065) : le
        // service ne doit pas avoir été touché.
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void leManagerQualiteCocheUnLivrable() throws Exception {
        when(service.completerLivrable(eq(PROJET), eq(PHASE), eq(LIVRABLE), any(), any()))
                .thenReturn(cycle());

        mockMvc.perform(put(RACINE + "/" + PHASE + "/deliverables/" + LIVRABLE + "/completion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true,\"comment\":\"reçu par courriel\","
                                + "\"owner\":\"A. Dupont\",\"dueDate\":\"2026-11-30\"}"))
                .andExpect(status().isOk())
                // La réponse porte le cycle entier : cocher un livrable change le
                // compte du dossier PPAP.
                .andExpect(jsonPath("$.ppapTotal").value(1));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unRenvoiAMoitiePoseRend422() throws Exception {
        when(service.completerLivrable(eq(PROJET), eq(PHASE), eq(LIVRABLE), any(), any()))
                .thenThrow(new ApqpDeliverableValidationException(
                        "A record reference needs both its kind and its id"));

        mockMvc.perform(put(RACINE + "/" + PHASE + "/deliverables/" + LIVRABLE + "/completion")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true,\"linkedKind\":\"FMEA\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid APQP Deliverable Content"));
    }

    // ---------- réinitialisation ----------

    @Test
    @WithMockUser(roles = "USER")
    void unOperateurNeReinitialisePasLeCycle() throws Exception {
        mockMvc.perform(post(RACINE + "/reset").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurDuClientReinitialiseLeCycle() throws Exception {
        when(service.reinitialiser(PROJET)).thenReturn(cycle());

        mockMvc.perform(post(RACINE + "/reset").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phases[0].title").value("Conception du processus"));
    }

    // ---------- ce que rendent les refus du service ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unProjetInconnuRend404() throws Exception {
        when(service.cycle(PROJET)).thenThrow(new ApqpProjectNotFoundException(PROJET));

        mockMvc.perform(get(RACINE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("APQP Element Not Found"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unePhaseInconnueRend404() throws Exception {
        doThrow(new ApqpPhaseNotFoundException(PHASE))
                .when(service).supprimerPhase(PROJET, PHASE);

        mockMvc.perform(delete(RACINE + "/" + PHASE).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("APQP Element Not Found"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unLivrableInconnuRend404() throws Exception {
        when(service.supprimerLivrable(PROJET, PHASE, LIVRABLE))
                .thenThrow(new ApqpDeliverableNotFoundException(LIVRABLE));

        mockMvc.perform(delete(RACINE + "/" + PHASE + "/deliverables/" + LIVRABLE).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unOrdrePartielRend422EtNon400() throws Exception {
        // La requête est bien formée — une liste d'identifiants — mais elle ne
        // décrit pas le cycle. C'est le contenu qui est refusé, pas la syntaxe.
        when(service.reorganiser(eq(PROJET), any())).thenThrow(new ApqpReorderException());

        mockMvc.perform(put(RACINE + "/order")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phaseIds\":[\"" + PHASE + "\"]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Invalid APQP Reorder"));
    }
}
