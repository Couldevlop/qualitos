package com.openlab.qualitos.quality.ideas.infrastructure;

import com.openlab.qualitos.quality.circle.CircleProposal;
import com.openlab.qualitos.quality.circle.QualityCircleRepository;
import com.openlab.qualitos.quality.ideas.domain.Idea;
import com.openlab.qualitos.quality.ideas.domain.IdeaNotFoundException;
import com.openlab.qualitos.quality.ideas.domain.IdeaRepository;
import com.openlab.qualitos.quality.ideas.domain.IdeaStateException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>{@code @Transactional} est nécessaire ici, pas cosmétique : {@code circle}
 * est {@code LAZY} sur {@code CircleProposal}, {@code open-in-view} est
 * désactivé, et sans une transaction couvrant tout le corps de la méthode,
 * chaque appel de dépôt Spring Data ouvre puis referme sa propre transaction.
 * {@code IdeaMapper.toDomain} lit alors {@code p.getCircle().getId()} sur une
 * entité déjà détachée — {@code LazyInitializationException}. Le module voisin
 * {@code CircleService} porte la même annotation pour la même raison.
 */
@Component
@Transactional(readOnly = true)
public class IdeaRepositoryAdapter implements IdeaRepository {

    private final IdeaJpaRepository jpa;
    private final QualityCircleRepository circles;

    public IdeaRepositoryAdapter(IdeaJpaRepository jpa, QualityCircleRepository circles) {
        this.jpa = jpa;
        this.circles = circles;
    }

    @Override
    @Transactional
    public Idea save(Idea idea) {
        CircleProposal existing = null;
        if (idea.getId() != null) {
            // Un identifiant fourni et introuvable n'est pas une création : c'est
            // une transition sur une idée disparue entre-temps (supprimée par un
            // autre acteur). La confondre avec une création ressusciterait une
            // ligne neuve sous l'identifiant de l'ancienne, au lieu de rendre 404.
            existing = jpa.findByIdAndTenantId(idea.getId(), idea.getTenantId())
                    .orElseThrow(() -> new IdeaNotFoundException(idea.getId()));
        }

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
