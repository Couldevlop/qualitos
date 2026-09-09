package com.openlab.qualitos.quality.ideas.infrastructure;

import com.openlab.qualitos.quality.circle.CircleProposal;
import com.openlab.qualitos.quality.circle.ProposalStatus;
import com.openlab.qualitos.quality.ideas.domain.Idea;
import com.openlab.qualitos.quality.ideas.domain.IdeaStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La traduction entre la ligne héritée et l'idée.
 *
 * <p>C'est le prix de la Clean Architecture ici : le domaine ignore JPA, donc
 * quelqu'un doit traduire. Ce banc existe parce qu'un mapper muet est le plus
 * sûr moyen de perdre une colonne sans que rien ne le signale.
 */
class IdeaMapperTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID AUTEUR = UUID.randomUUID();
    private static final Instant T0 = Instant.parse("2026-09-09T08:00:00Z");

    @Test
    @DisplayName("une proposition devient une idée sans rien perdre")
    void versLeDomaine() {
        CircleProposal ligne = new CircleProposal();
        ligne.setId(UUID.randomUUID());
        ligne.setTenantId(TENANT);
        ligne.setTitle("Eclairage LED atelier");
        ligne.setDescription("Moins de rebuts");
        ligne.setStatus(ProposalStatus.UNDER_REVIEW);
        ligne.setProposedBy(AUTEUR);
        ligne.setProposedByName("M. Alaoui");
        ligne.setCreatedAt(T0);
        ligne.setUpdatedAt(T0);

        Idea idee = IdeaMapper.toDomain(ligne);

        assertThat(idee.getId()).isEqualTo(ligne.getId());
        assertThat(idee.getTenantId()).isEqualTo(TENANT);
        assertThat(idee.getCircleId()).isNull();
        assertThat(idee.getTitle()).isEqualTo("Eclairage LED atelier");
        assertThat(idee.getStatus()).isEqualTo(IdeaStatus.UNDER_REVIEW);
        assertThat(idee.getProposedBy()).isEqualTo(AUTEUR);
        assertThat(idee.getProposedByName()).isEqualTo("M. Alaoui");
    }

    @Test
    @DisplayName("une idée neuve devient une ligne sans cercle")
    void versLaLigne() {
        Idea idee = Idea.submitted(TENANT, null, "Bac de tri", null, AUTEUR, "M. Alaoui", T0);

        CircleProposal ligne = IdeaMapper.toEntity(idee, null, null);

        assertThat(ligne.getTenantId()).isEqualTo(TENANT);
        assertThat(ligne.getCircle()).isNull();
        assertThat(ligne.getStatus()).isEqualTo(ProposalStatus.PROPOSED);
        assertThat(ligne.getDescription()).isNull();
    }

    @Test
    @DisplayName("les six statuts se traduisent dans les deux sens")
    void statuts() {
        // Deux enums face à face : le jour où l'un gagne une valeur, ce banc est
        // le seul endroit qui s'en apercevra.
        for (IdeaStatus statut : IdeaStatus.values()) {
            ProposalStatus traduit = IdeaMapper.toProposalStatus(statut);
            assertThat(IdeaMapper.toIdeaStatus(traduit)).isEqualTo(statut);
        }
        assertThat(ProposalStatus.values()).hasSameSizeAs(IdeaStatus.values());
    }
}
