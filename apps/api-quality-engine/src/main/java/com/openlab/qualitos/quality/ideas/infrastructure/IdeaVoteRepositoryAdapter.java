package com.openlab.qualitos.quality.ideas.infrastructure;

import com.openlab.qualitos.quality.ideas.domain.IdeaVote;
import com.openlab.qualitos.quality.ideas.domain.IdeaVoteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class IdeaVoteRepositoryAdapter implements IdeaVoteRepository {

    private final ProposalVoteJpaRepository jpa;

    public IdeaVoteRepositoryAdapter(ProposalVoteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void add(IdeaVote vote) {
        ProposalVoteId id = new ProposalVoteId(vote.getIdeaId(), vote.getVoterId());
        try {
            jpa.save(new ProposalVoteJpaEntity(id, vote.getTenantId(), vote.getCastAt()));
        } catch (DataIntegrityViolationException alreadyThere) {
            // Deux clics simultanés : la clé primaire a tranché, et le résultat
            // voulu — une voix — est atteint. Rien à signaler à l'appelant.
        }
    }

    @Override
    public boolean remove(UUID ideaId, UUID voterId) {
        ProposalVoteId id = new ProposalVoteId(ideaId, voterId);
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }

    @Override
    public Map<UUID, Long> countByIdea(UUID tenantId) {
        Map<UUID, Long> counts = new HashMap<>();
        for (Object[] row : jpa.countByIdea(tenantId)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public Set<UUID> ideasVotedBy(UUID tenantId, UUID voterId) {
        return new HashSet<>(jpa.ideasVotedBy(tenantId, voterId));
    }
}
