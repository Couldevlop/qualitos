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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
 * Le partage des rôles sur les projets APQP.
 *
 * <p>Même frontière que le reste du module : consulter les programmes en cours
 * est ouvert — chacun doit savoir ce qu'on attend de lui —, mais en ouvrir ou
 * en supprimer un est un acte de pilotage. Supprimer emporte le cycle, ses
 * livrables et les pièces qui les prouvent.
 */
@Tag("web")
@WebMvcTest(controllers = ApqpProjectController.class)
@Import(MethodSecurityTestConfig.class)
class ApqpProjectControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ApqpProjectService service;

    private static final UUID PROJET = UUID.randomUUID();
    private static final String RACINE = "/api/v1/apqp/projects";

    private static final String CORPS = """
            {"name":"Support moteur 2027","type":"NPI","customer":"Client A",
             "reference":"PRG-2027-014"}""";

    private static ApqpDto.ProjectResponse projet() {
        return new ApqpDto.ProjectResponse(
                PROJET, "Support moteur 2027", ApqpProjectType.NPI, "Client A",
                "PRG-2027-014", null, 48, 6, 12, 2,
                Instant.parse("2026-09-01T08:00:00Z"), Instant.parse("2026-09-10T08:00:00Z"));
    }

    // ---------- lecture ----------

    @Test
    @WithMockUser(roles = "USER")
    void toutUtilisateurAuthentifieListeLesProjets() throws Exception {
        when(service.lister()).thenReturn(List.of(projet()));

        mockMvc.perform(get(RACINE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Support moteur 2027"))
                .andExpect(jsonPath("$[0].type").value("NPI"))
                // Les compteurs voyagent avec le projet : la liste dit où en est
                // chaque programme sans un appel par ligne.
                .andExpect(jsonPath("$[0].deliverablesTotal").value(48))
                .andExpect(jsonPath("$[0].ppapDone").value(2));
    }

    @Test
    @WithAnonymousUser
    void unVisiteurAnonymeNeListeRien() throws Exception {
        mockMvc.perform(get(RACINE))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // ---------- écriture : qui peut ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void leManagerQualiteOuvreUnProjet() throws Exception {
        when(service.creer(any(), any())).thenReturn(projet());

        mockMvc.perform(post(RACINE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROJET.toString()));
    }

    @Test
    @WithMockUser(roles = "DIRECTOR_QUALITY")
    void leDirecteurQualiteCorrigeUnProjet() throws Exception {
        when(service.modifier(any(), any())).thenReturn(projet());

        mockMvc.perform(put(RACINE + "/" + PROJET)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS))
                .andExpect(status().isOk());
    }

    // ---------- écriture : qui ne peut pas ----------

    @Test
    @WithMockUser(roles = "USER")
    void unOperateurNOuvrePasDeProjet() throws Exception {
        mockMvc.perform(post(RACINE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void unAuditeurNeSupprimePasUnProjet() throws Exception {
        // La suppression emporte le cycle, ses livrables et leurs preuves : c'est
        // l'écriture dont le refus compte le plus.
        mockMvc.perform(delete(RACINE + "/" + PROJET).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "USER")
    void unCorpsMalformeSansLeRoleResteUnRefus() throws Exception {
        // L'autorisation se décide AVANT la lecture du corps (ADR 0065), sans
        // quoi un point d'entrée fermé deviendrait un oracle de validation.
        mockMvc.perform(post(RACINE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    // ---------- validation de surface ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unProjetSansTypeEstRefuseALaFrontiere() throws Exception {
        // Le type décide de ce qu'on attend du programme, et c'est sur lui qu'on
        // filtre la liste : le deviner de l'intitulé ne marcherait pas.
        mockMvc.perform(post(RACINE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Support moteur\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unTypeInconnuEstRefuseALaFrontiere() throws Exception {
        mockMvc.perform(post(RACINE)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Support moteur\",\"type\":\"PROTOTYPE\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    // ---------- ce que rendent les refus du service ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void leProjetDUnAutreClientRend404EtNon403() throws Exception {
        // Distinguer « il n'existe pas » de « il ne vous appartient pas » dirait à
        // qui essaie qu'un projet de ce nom existe ailleurs (OWASP A01).
        doThrow(new ApqpProjectNotFoundException(PROJET)).when(service).supprimer(PROJET);

        mockMvc.perform(delete(RACINE + "/" + PROJET).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("APQP Element Not Found"));
    }
}
