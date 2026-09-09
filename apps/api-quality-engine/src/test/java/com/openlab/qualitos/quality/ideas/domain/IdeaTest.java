package com.openlab.qualitos.quality.ideas.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'idée et sa fenêtre de vote.
 *
 * <p>Le vote se ferme dès que l'idée est tranchée : après coup, le compteur doit
 * dire l'adhésion AU MOMENT où l'on a décidé. Le laisser bouger réécrirait la
 * base d'une décision déjà prise, et c'est la seule règle de ce module qu'aucun
 * écran ne rattraperait.
 */
class IdeaTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID AUTEUR = UUID.randomUUID();
    private static final UUID ARBITRE = UUID.randomUUID();
    private static final Instant T0 = Instant.parse("2026-09-09T08:00:00Z");

    private Idea deposee() {
        return Idea.submitted(TENANT, null, "Eclairage LED atelier", "Moins de rebuts",
                AUTEUR, "M. Alaoui", T0);
    }

    @Test
    @DisplayName("une idée naît déposée, sans cercle, au nom de son auteur")
    void depot() {
        Idea idee = deposee();

        assertThat(idee.getStatus()).isEqualTo(IdeaStatus.PROPOSED);
        assertThat(idee.getCircleId()).isNull();
        assertThat(idee.getProposedBy()).isEqualTo(AUTEUR);
        assertThat(idee.getProposedByName()).isEqualTo("M. Alaoui");
        assertThat(idee.getTenantId()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("le vote est ouvert tant que l'idée n'est pas tranchée")
    void fenetreDeVote() {
        assertThat(IdeaStatus.PROPOSED.voteOpen()).isTrue();
        assertThat(IdeaStatus.UNDER_REVIEW.voteOpen()).isTrue();
        assertThat(IdeaStatus.APPROVED.voteOpen()).isFalse();
        assertThat(IdeaStatus.REJECTED.voteOpen()).isFalse();
        assertThat(IdeaStatus.IMPLEMENTED.voteOpen()).isFalse();
        assertThat(IdeaStatus.MEASURED.voteOpen()).isFalse();
    }

    @Test
    @DisplayName("le parcours complet mène de déposée à mesurée")
    void parcoursComplet() {
        Idea idee = deposee();
        idee.review(T0);
        idee.approve(ARBITRE, T0);
        idee.implement(T0);
        idee.measure("Rebuts -12 %", T0);

        assertThat(idee.getStatus()).isEqualTo(IdeaStatus.MEASURED);
        assertThat(idee.getImpactNote()).isEqualTo("Rebuts -12 %");
        assertThat(idee.getValidatedBy()).isEqualTo(ARBITRE);
    }

    @Test
    @DisplayName("on ne retient pas une idée qu'on n'a pas étudiée")
    void approbationHorsSequence() {
        Idea idee = deposee();

        assertThatThrownBy(() -> idee.approve(ARBITRE, T0))
                .isInstanceOf(IdeaStateException.class);
    }

    @Test
    @DisplayName("nul ne retient sa propre idée")
    void arbitreNEstPasLAuteur() {
        // La garde vaut ce que vaut l'identité : elle n'a de sens que parce que
        // l'arbitre vient du jeton et non du corps de la requête.
        Idea idee = deposee();
        idee.review(T0);

        assertThatThrownBy(() -> idee.approve(AUTEUR, T0))
                .isInstanceOf(IdeaStateException.class)
                .hasMessageContaining("proposer");
    }

    @Test
    @DisplayName("écarter une idée exige un motif")
    void refusMotive() {
        Idea idee = deposee();

        assertThatThrownBy(() -> idee.reject(ARBITRE, "  ", T0))
                .isInstanceOf(IdeaStateException.class);
    }

    @Test
    @DisplayName("une idée réalisée ne se réécrit pas")
    void pasDeRetourEnArriere() {
        Idea idee = deposee();
        idee.review(T0);
        idee.approve(ARBITRE, T0);
        idee.implement(T0);

        assertThatThrownBy(() -> idee.reject(ARBITRE, "trop tard", T0))
                .isInstanceOf(IdeaStateException.class);
    }
}
