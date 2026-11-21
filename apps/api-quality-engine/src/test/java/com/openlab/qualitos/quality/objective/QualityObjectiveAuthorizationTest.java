package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.config.SecurityConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Qui a le droit de toucher à un objectif qualité.
 *
 * <p>La frontière est la même que pour les indicateurs, et pour la même raison :
 * fixer une cible est une DÉCISION, et déplacer la cible d'un objectif qu'on
 * n'atteint pas le fait passer au vert sans que rien n'ait changé sur le terrain
 * — exactement ce qu'un auditeur vient chercher au §6.2 et au §9.3.
 *
 * <p>Accepter ou rejeter une CAPA proposée en relève aussi : c'est l'arbitrage du
 * pilote, pas une consultation. Un utilisateur qui pourrait rejeter les
 * propositions ferait disparaître des écarts du registre sans en répondre.
 *
 * <p>LIRE reste ouvert à tout authentifié : la revue des objectifs est ce qu'une
 * organisation montre à ses équipes.
 *
 * <p>Monté sur la VRAIE chaîne de filtres ({@link SecurityConfig}) : c'est la
 * seule façon de voir l'alias de rôle du directeur qualité, et de distinguer un
 * 401 d'un 403.
 */
@Tag("web")
@WebMvcTest(controllers = QualityObjectiveController.class)
@Import(SecurityConfig.class)
class QualityObjectiveAuthorizationTest {

    static final String BASE = "/api/v1/quality-objectives";
    static final UUID OBJ = UUID.randomUUID();
    static final UUID PROP = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean QualityObjectiveService service;

    // ---------- la décision : réservée au pilotage ----------

    @Test @WithMockUser(roles = "USER")
    void unUtilisateurNeDefinitPasUnObjectif() throws Exception {
        mockMvc.perform(post(BASE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verify(service, never()).create(any());
    }

    @Test @WithMockUser(roles = "USER")
    void unUtilisateurNeDeplacePasLaCible() throws Exception {
        mockMvc.perform(patch(BASE + "/" + OBJ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetValue\":9999}"))
                .andExpect(status().isForbidden());
        verify(service, never()).update(any(), any());
    }

    @Test @WithMockUser(roles = "USER")
    void unUtilisateurNArchivePasUnObjectif() throws Exception {
        mockMvc.perform(delete(BASE + "/" + OBJ).with(csrf()))
                .andExpect(status().isForbidden());
        verify(service, never()).archive(any());
    }

    @Test @WithMockUser(roles = "USER")
    void unUtilisateurNAccepteNiNeRejetteUneCapaProposee() throws Exception {
        mockMvc.perform(post(BASE + "/proposals/" + PROP + "/accept").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/proposals/" + PROP + "/reject").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"peu importe\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).accept(any());
        verify(service, never()).reject(any(), any());
    }

    @Test @WithMockUser(roles = "USER")
    void unUtilisateurNeDeclenchePasLaRegle() throws Exception {
        mockMvc.perform(post(BASE + "/" + OBJ + "/evaluate").with(csrf()))
                .andExpect(status().isForbidden());
        verify(service, never()).evaluate(any());
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void unAuditeurConstate_maisNeDefinitRien() throws Exception {
        // L'auditeur a la lecture la plus large de la plateforme et aucune main
        // sur ce qu'il audite : fixer la cible qu'il viendra vérifier serait
        // juger son propre travail.
        mockMvc.perform(patch(BASE + "/" + OBJ).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/proposals/" + PROP + "/accept").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void lePilotageDefinit_etTranche() throws Exception {
        mockMvc.perform(delete(BASE + "/" + OBJ).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).archive(OBJ);
    }

    // ---------- la lecture : ouverte à tout authentifié ----------

    @Test @WithMockUser(roles = "USER")
    void unUtilisateurLitLaRevue() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isOk());
        verify(service).review(null, null);
    }

    @Test @WithMockUser(roles = "USER")
    void unUtilisateurLitLHistoriqueDesPropositions() throws Exception {
        mockMvc.perform(get(BASE + "/" + OBJ + "/proposals")).andExpect(status().isOk());
        verify(service).history(OBJ);
    }

    @Test
    void sansJeton_rienNEstLisible() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        verify(service, never()).review(any(), any());
    }
}
