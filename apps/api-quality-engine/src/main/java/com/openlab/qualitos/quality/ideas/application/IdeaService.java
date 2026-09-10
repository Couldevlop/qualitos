package com.openlab.qualitos.quality.ideas.application;

import com.openlab.qualitos.quality.ideas.domain.Idea;
import com.openlab.qualitos.quality.ideas.domain.IdeaNotFoundException;
import com.openlab.qualitos.quality.ideas.domain.IdeaRepository;
import com.openlab.qualitos.quality.ideas.domain.IdeaStatus;
import com.openlab.qualitos.quality.ideas.domain.IdeaVote;
import com.openlab.qualitos.quality.ideas.domain.IdeaVoteRepository;
import com.openlab.qualitos.quality.ideas.domain.VoteClosedException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La boîte à idées : déposer, soutenir, arbitrer.
 *
 * <p>Sans Spring ni JPA — la couche application ne dépend que de ports. Ce qui
 * la rend testable sans contexte, et c'est là que vivent les deux règles du
 * module : l'acteur vient du contexte authentifié, et le vote se ferme dès que
 * l'idée est tranchée.
 */
public class IdeaService {

    /** L'ordre du cycle, et donc celui des colonnes de l'écran. */
    private static final List<IdeaStatus> COLONNES = List.of(
            IdeaStatus.PROPOSED, IdeaStatus.UNDER_REVIEW, IdeaStatus.APPROVED,
            IdeaStatus.IMPLEMENTED, IdeaStatus.MEASURED, IdeaStatus.REJECTED);

    private final IdeaRepository ideas;
    private final IdeaVoteRepository votes;
    private final TenantProvider tenants;
    private final ActorProvider actors;
    private final Clock clock;

    public IdeaService(IdeaRepository ideas, IdeaVoteRepository votes,
                       TenantProvider tenants, ActorProvider actors, Clock clock) {
        this.ideas = ideas;
        this.votes = votes;
        this.tenants = tenants;
        this.actors = actors;
        this.clock = clock;
    }

    public IdeaDto.BoardView board() {
        UUID tenant = tenants.requireTenantId();
        UUID moi = actors.requireActorId();

        List<Idea> toutes = ideas.findByTenant(tenant);
        Map<UUID, Long> compte = votes.countByIdea(tenant);
        Set<UUID> miennes = votes.ideasVotedBy(tenant, moi);

        Map<IdeaStatus, List<Idea>> parStatut = toutes.stream()
                .collect(Collectors.groupingBy(Idea::getStatus));

        List<IdeaDto.ColumnView> colonnes = new ArrayList<>();
        for (IdeaStatus statut : COLONNES) {
            List<IdeaDto.IdeaView> vues = parStatut.getOrDefault(statut, List.of()).stream()
                    .map(i -> vue(i, compte, miennes))
                    .toList();
            colonnes.add(new IdeaDto.ColumnView(statut, vues));
        }
        return new IdeaDto.BoardView(colonnes);
    }

    public IdeaDto.IdeaView submit(IdeaDto.SubmitCommand commande) {
        UUID tenant = tenants.requireTenantId();
        Idea idee = Idea.submitted(tenant, commande.circleId(), commande.title(),
                commande.description(), actors.requireActorId(),
                actors.actorDisplayName().orElse(null), now());
        return vue(ideas.save(idee));
    }

    public IdeaDto.IdeaView vote(UUID ideaId) {
        UUID tenant = tenants.requireTenantId();
        Idea idee = charger(ideaId, tenant);
        if (!idee.voteOpen()) {
            throw new VoteClosedException(ideaId);
        }
        votes.add(IdeaVote.cast(ideaId, actors.requireActorId(), tenant, now()));
        return vue(idee);
    }

    public IdeaDto.IdeaView unvote(UUID ideaId) {
        UUID tenant = tenants.requireTenantId();
        Idea idee = charger(ideaId, tenant);
        // Retirer sa voix d'une idée tranchée est refusé pour la même raison
        // qu'en donner une : le compteur dit l'adhésion au moment du choix.
        if (!idee.voteOpen()) {
            throw new VoteClosedException(ideaId);
        }
        votes.remove(ideaId, actors.requireActorId());
        return vue(idee);
    }

    public IdeaDto.IdeaView review(UUID ideaId) {
        return transition(ideaId, idee -> { idee.review(now()); return idee; });
    }

    public IdeaDto.IdeaView approve(UUID ideaId) {
        UUID arbitre = actors.requireActorId();
        return transition(ideaId, idee -> { idee.approve(arbitre, now()); return idee; });
    }

    public IdeaDto.IdeaView reject(UUID ideaId, IdeaDto.RejectCommand commande) {
        UUID arbitre = actors.requireActorId();
        return transition(ideaId,
                idee -> { idee.reject(arbitre, commande.reason(), now()); return idee; });
    }

    public IdeaDto.IdeaView implement(UUID ideaId) {
        return transition(ideaId, idee -> { idee.implement(now()); return idee; });
    }

    public IdeaDto.IdeaView measure(UUID ideaId, IdeaDto.ImpactCommand commande) {
        return transition(ideaId,
                idee -> { idee.measure(commande.impactNote(), now()); return idee; });
    }

    // ---------- interne ----------

    private IdeaDto.IdeaView transition(UUID ideaId, Function<Idea, Idea> geste) {
        UUID tenant = tenants.requireTenantId();
        Idea idee = charger(ideaId, tenant);
        return vue(ideas.save(geste.apply(idee)));
    }

    private Idea charger(UUID ideaId, UUID tenant) {
        return ideas.findByIdAndTenant(ideaId, tenant)
                .orElseThrow(() -> new IdeaNotFoundException(ideaId));
    }

    private IdeaDto.IdeaView vue(Idea idee) {
        UUID tenant = idee.getTenantId();
        return vue(idee, votes.countByIdea(tenant),
                votes.ideasVotedBy(tenant, actors.requireActorId()));
    }

    private IdeaDto.IdeaView vue(Idea idee, Map<UUID, Long> compte, Set<UUID> miennes) {
        return new IdeaDto.IdeaView(
                idee.getId(), idee.getTitle(), idee.getDescription(), idee.getStatus(),
                idee.getProposedBy(), idee.getProposedByName(),
                compte.getOrDefault(idee.getId(), 0L),
                miennes.contains(idee.getId()), idee.voteOpen(), idee.getCircleId(),
                idee.getRejectionReason(), idee.getImpactNote(), idee.getCreatedAt());
    }

    private Instant now() { return clock.instant(); }
}
