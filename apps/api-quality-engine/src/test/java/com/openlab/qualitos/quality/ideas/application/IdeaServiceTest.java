package com.openlab.qualitos.quality.ideas.application;

import com.openlab.qualitos.quality.ideas.domain.Idea;
import com.openlab.qualitos.quality.ideas.domain.IdeaNotFoundException;
import com.openlab.qualitos.quality.ideas.domain.IdeaRepository;
import com.openlab.qualitos.quality.ideas.domain.IdeaStatus;
import com.openlab.qualitos.quality.ideas.domain.IdeaVote;
import com.openlab.qualitos.quality.ideas.domain.IdeaVoteRepository;
import com.openlab.qualitos.quality.ideas.domain.VoteClosedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La boîte à idées.
 *
 * <p>Deux exigences se croisent ici. L'acteur vient TOUJOURS du contexte
 * authentifié : une idée déposée au nom d'un autre, ou un arbitre auto-proclamé,
 * feraient de l'écran une signature falsifiable. Et le vote se ferme à la
 * décision : après coup, le compteur dit l'adhésion au moment du choix.
 *
 * <p>Les doublures sont écrites à la main plutôt que simulées : le décompte des
 * voix et l'isolation par client sont justement ce qu'un mock rendrait vrai par
 * construction.
 */
class IdeaServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID AUTRE_TENANT = UUID.randomUUID();
    private static final UUID MOI = UUID.randomUUID();
    private static final UUID QUELQUUN = UUID.randomUUID();
    private static final Instant T0 = Instant.parse("2026-09-09T08:00:00Z");

    private DepotEnMemoire idees;
    private VotesEnMemoire votes;
    private IdeaService service;
    private UUID acteur;

    @BeforeEach
    void poser() {
        idees = new DepotEnMemoire();
        votes = new VotesEnMemoire();
        acteur = MOI;
        service = new IdeaService(idees, votes, () -> TENANT, new ActeurCourant(),
                Clock.fixed(T0, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("déposer une idée l'attribue à l'utilisateur connecté, pas au corps de la requête")
    void depotAttribueAuJeton() {
        IdeaDto.IdeaView vue = service.submit(
                new IdeaDto.SubmitCommand("Eclairage LED atelier", "Moins de rebuts", null));

        assertThat(vue.authorId()).isEqualTo(MOI);
        assertThat(vue.authorName()).isEqualTo("M. Alaoui");
        assertThat(vue.status()).isEqualTo(IdeaStatus.PROPOSED);
        assertThat(vue.votes()).isZero();
    }

    @Test
    @DisplayName("une voix compte une fois, même en cliquant deux fois")
    void voixUnique() {
        UUID id = service.submit(new IdeaDto.SubmitCommand("Bac de tri", null, null)).id();

        service.vote(id);
        IdeaDto.IdeaView vue = service.vote(id);

        assertThat(vue.votes()).isEqualTo(1);
        assertThat(vue.votedByMe()).isTrue();
    }

    @Test
    @DisplayName("on retire sa voix, et le compteur redescend")
    void retraitDeVoix() {
        UUID id = service.submit(new IdeaDto.SubmitCommand("Bac de tri", null, null)).id();
        service.vote(id);

        IdeaDto.IdeaView vue = service.unvote(id);

        assertThat(vue.votes()).isZero();
        assertThat(vue.votedByMe()).isFalse();
    }

    @Test
    @DisplayName("le vote se ferme dès que l'idée est tranchée")
    void voteFermeApresDecision() {
        UUID id = service.submit(new IdeaDto.SubmitCommand("Poste de reglage rapide", null, null)).id();
        service.review(id);
        acteur = QUELQUUN;          // l'arbitre n'est pas l'auteur
        service.approve(id);

        assertThatThrownBy(() -> service.vote(id)).isInstanceOf(VoteClosedException.class);
    }

    @Test
    @DisplayName("l'idée d'un autre client reste introuvable")
    void isolationParClient() {
        Idea ailleurs = Idea.submitted(AUTRE_TENANT, null, "Idee d'ailleurs", null,
                QUELQUUN, "S. Benali", T0);
        ailleurs.assignId(UUID.randomUUID());
        idees.enregistrerBrut(ailleurs);

        assertThatThrownBy(() -> service.vote(ailleurs.getId()))
                .isInstanceOf(IdeaNotFoundException.class);
    }

    @Test
    @DisplayName("le tableau range chaque idée dans sa colonne, avec ses voix")
    void tableau() {
        UUID soumise = service.submit(new IdeaDto.SubmitCommand("Soumise", null, null)).id();
        UUID etude = service.submit(new IdeaDto.SubmitCommand("A l'etude", null, null)).id();
        service.review(etude);
        service.vote(etude);

        IdeaDto.BoardView tableau = service.board();

        assertThat(colonne(tableau, IdeaStatus.PROPOSED))
                .extracting(IdeaDto.IdeaView::id).containsExactly(soumise);
        assertThat(colonne(tableau, IdeaStatus.UNDER_REVIEW))
                .singleElement()
                .satisfies(v -> {
                    assertThat(v.id()).isEqualTo(etude);
                    assertThat(v.votes()).isEqualTo(1);
                    assertThat(v.votedByMe()).isTrue();
                });
    }

    @Test
    @DisplayName("nul ne retient sa propre idée")
    void arbitreNEstPasLAuteur() {
        UUID id = service.submit(new IdeaDto.SubmitCommand("La mienne", null, null)).id();
        service.review(id);

        assertThatThrownBy(() -> service.approve(id))
                .hasMessageContaining("proposer");
    }

    @Test
    @DisplayName("écarter une idée consigne son motif")
    void refusMotive() {
        UUID id = service.submit(new IdeaDto.SubmitCommand("Trop couteuse", null, null)).id();
        acteur = QUELQUUN;

        IdeaDto.IdeaView vue = service.reject(id, new IdeaDto.RejectCommand("Hors budget 2026"));

        assertThat(vue.status()).isEqualTo(IdeaStatus.REJECTED);
        assertThat(vue.rejectionReason()).isEqualTo("Hors budget 2026");
    }

    // ---------- doublures ----------

    private static List<IdeaDto.IdeaView> colonne(IdeaDto.BoardView tableau, IdeaStatus statut) {
        return tableau.columns().stream()
                .filter(c -> c.status() == statut)
                .findFirst()
                .orElseThrow()
                .ideas();
    }

    /** L'acteur courant, que les bancs déplacent en écrivant dans `acteur`. */
    private final class ActeurCourant implements ActorProvider {
        @Override public UUID requireActorId() { return acteur; }
        @Override public Optional<String> actorDisplayName() {
            return Optional.of(acteur.equals(MOI) ? "M. Alaoui" : "S. Benali");
        }
    }

    private static final class DepotEnMemoire implements IdeaRepository {
        private final Map<UUID, Idea> parId = new HashMap<>();

        @Override public Idea save(Idea idea) {
            if (idea.getId() == null) idea.assignId(UUID.randomUUID());
            parId.put(idea.getId(), idea);
            return idea;
        }
        @Override public Optional<Idea> findByIdAndTenant(UUID id, UUID tenantId) {
            return Optional.ofNullable(parId.get(id))
                    .filter(i -> i.getTenantId().equals(tenantId));
        }
        @Override public List<Idea> findByTenant(UUID tenantId) {
            return parId.values().stream().filter(i -> i.getTenantId().equals(tenantId)).toList();
        }
        void enregistrerBrut(Idea idea) { parId.put(idea.getId(), idea); }
    }

    private static final class VotesEnMemoire implements IdeaVoteRepository {
        private final List<IdeaVote> lignes = new ArrayList<>();

        @Override public void add(IdeaVote vote) {
            boolean deja = lignes.stream().anyMatch(v ->
                    v.getIdeaId().equals(vote.getIdeaId()) && v.getVoterId().equals(vote.getVoterId()));
            if (!deja) lignes.add(vote);
        }
        @Override public boolean remove(UUID ideaId, UUID voterId) {
            return lignes.removeIf(v ->
                    v.getIdeaId().equals(ideaId) && v.getVoterId().equals(voterId));
        }
        @Override public Map<UUID, Long> countByIdea(UUID tenantId) {
            Map<UUID, Long> counts = new HashMap<>();
            lignes.stream().filter(v -> v.getTenantId().equals(tenantId))
                    .forEach(v -> counts.merge(v.getIdeaId(), 1L, Long::sum));
            return counts;
        }
        @Override public Set<UUID> ideasVotedBy(UUID tenantId, UUID voterId) {
            Set<UUID> ids = new HashSet<>();
            lignes.stream()
                    .filter(v -> v.getTenantId().equals(tenantId) && v.getVoterId().equals(voterId))
                    .forEach(v -> ids.add(v.getIdeaId()));
            return ids;
        }
    }
}
