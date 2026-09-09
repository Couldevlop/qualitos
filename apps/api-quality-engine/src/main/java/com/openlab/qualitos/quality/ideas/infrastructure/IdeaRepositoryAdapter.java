package com.openlab.qualitos.quality.ideas.infrastructure;

import com.openlab.qualitos.quality.circle.CircleProposal;
import com.openlab.qualitos.quality.circle.QualityCircleRepository;
import com.openlab.qualitos.quality.ideas.domain.Idea;
import com.openlab.qualitos.quality.ideas.domain.IdeaRepository;
import com.openlab.qualitos.quality.ideas.domain.IdeaStateException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class IdeaRepositoryAdapter implements IdeaRepository {

    private final IdeaJpaRepository jpa;
    private final QualityCircleRepository circles;

    public IdeaRepositoryAdapter(IdeaJpaRepository jpa, QualityCircleRepository circles) {
        this.jpa = jpa;
        this.circles = circles;
    }

    @Override
    public Idea save(Idea idea) {
        CircleProposal existing = idea.getId() == null ? null
                : jpa.findByIdAndTenantId(idea.getId(), idea.getTenantId()).orElse(null);

        var circle = idea.getCircleId() == null ? null
                : circles.findById(idea.getCircleId())
                        .filter(c -> c.getTenantId().equals(idea.getTenantId()))
                        // Le déclencheur en base refuserait aussi, mais après coup
                        // et dans un message illisible : on refuse ici, en clair.
                        .orElseThrow(() -> new IdeaStateException(
                                "The circle does not belong to this tenant"));

        CircleProposal saved = jpa.save(IdeaMapper.toEntity(idea, existing, circle));
        Idea out = IdeaMapper.toDomain(saved);
        return out;
    }

    @Override
    public Optional<Idea> findByIdAndTenant(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId).map(IdeaMapper::toDomain);
    }

    @Override
    public List<Idea> findByTenant(UUID tenantId) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(IdeaMapper::toDomain)
                .toList();
    }
}
