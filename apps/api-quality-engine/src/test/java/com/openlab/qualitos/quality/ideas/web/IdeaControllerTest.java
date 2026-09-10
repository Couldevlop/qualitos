package com.openlab.qualitos.quality.ideas.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.ideas.application.IdeaDto;
import com.openlab.qualitos.quality.ideas.application.IdeaService;
import com.openlab.qualitos.quality.ideas.domain.IdeaNotFoundException;
import com.openlab.qualitos.quality.ideas.domain.IdeaStatus;
import com.openlab.qualitos.quality.ideas.domain.VoteClosedException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le partage des rôles sur la boîte à idées.
 *
 * <p>DÉPOSER et VOTER sont ouverts à tout authentifié : une boîte à idées
 * réservée aux profils qualité n'est plus une boîte à idées. ARBITRER ne l'est
 * pas — retenir ou écarter engage l'organisation, et écarter efface une
 * proposition de la vue de tous.
 */
@Tag("web")
@WebMvcTest(controllers = IdeaController.class)
@Import(MethodSecurityTestConfig.class)
class IdeaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean IdeaService service;

    private static final UUID IDEE = UUID.randomUUID();
    private static final UUID AUTEUR = UUID.randomUUID();

    private static IdeaDto.IdeaView vue(IdeaStatus statut, long voix) {
        return new IdeaDto.IdeaView(IDEE, "Eclairage LED atelier", "Moins de rebuts",
                statut, AUTEUR, "M. Alaoui", voix, true, statut.voteOpen(), null, null, null,
                Instant.parse("2026-09-09T08:00:00Z"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutUtilisateurAuthentifieLitLeTableau() throws Exception {
        when(service.board()).thenReturn(new IdeaDto.BoardView(List.of(
                new IdeaDto.ColumnView(IdeaStatus.PROPOSED, List.of(vue(IdeaStatus.PROPOSED, 8))))));

        mockMvc.perform(get("/api/v1/ideas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0].status").value("PROPOSED"))
                .andExpect(jsonPath("$.columns[0].ideas[0].votes").value(8));
    }

    @Test
    @WithAnonymousUser
    void unVisiteurAnonymeNeLitRien() throws Exception {
        mockMvc.perform(get("/api/v1/ideas")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "USER")
    void unOperateurDeposeUneIdee() throws Exception {
        when(service.submit(any())).thenReturn(vue(IdeaStatus.PROPOSED, 0));

        mockMvc.perform(post("/api/v1/ideas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Eclairage LED atelier\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value(AUTEUR.toString()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void etIlVote() throws Exception {
        when(service.vote(IDEE)).thenReturn(vue(IdeaStatus.PROPOSED, 1));

        mockMvc.perform(post("/api/v1/ideas/" + IDEE + "/vote").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void maisIlN_arbitrePas() throws Exception {
        mockMvc.perform(patch("/api/v1/ideas/" + IDEE + "/approve").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void leManagerQualiteArbitre() throws Exception {
        when(service.review(IDEE)).thenReturn(vue(IdeaStatus.UNDER_REVIEW, 3));

        mockMvc.perform(patch("/api/v1/ideas/" + IDEE + "/review").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void ecarterExigeUnMotif() throws Exception {
        mockMvc.perform(patch("/api/v1/ideas/" + IDEE + "/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "USER")
    void unCorpsMalformeSansLeRoleResteUnRefus() throws Exception {
        // Défaut d'ORDRE : sans le pré-contrôle, la validation du corps
        // répondrait 400 avant que @PreAuthorize ne dise 403, et un point
        // d'entrée fermé deviendrait un oracle de validation (ADR 0065).
        mockMvc.perform(patch("/api/v1/ideas/" + IDEE + "/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "USER")
    void voterSurUneIdeeTrancheeRend409() throws Exception {
        when(service.vote(IDEE)).thenThrow(new VoteClosedException(IDEE));

        mockMvc.perform(post("/api/v1/ideas/" + IDEE + "/vote").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Vote Closed"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void uneIdeeInconnueRend404() throws Exception {
        when(service.vote(IDEE)).thenThrow(new IdeaNotFoundException(IDEE));

        mockMvc.perform(post("/api/v1/ideas/" + IDEE + "/vote").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void retirerSaVoix() throws Exception {
        when(service.unvote(eq(IDEE))).thenReturn(vue(IdeaStatus.PROPOSED, 0));

        mockMvc.perform(delete("/api/v1/ideas/" + IDEE + "/vote").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votes").value(0));
    }
}
