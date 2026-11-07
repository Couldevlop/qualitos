package com.openlab.qualitos.quality.capa.effectiveness.infrastructure;

import com.openlab.qualitos.quality.capa.effectiveness.application.EffectivenessReviewPort;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * L'historique des évaluations d'efficacité, en base (ADR 0108).
 *
 * <p>Le tenant est toujours un ARGUMENT, jamais relu depuis le contexte ici :
 * cet adaptateur ne décide pas de quel client il parle, il obéit. C'est la
 * couche application qui lit le jeton (CLAUDE.md §18.2 #2), et l'avoir en
 * paramètre rend impossible la requête qui aurait « oublié » le filtre.
 */
@Component
public class EffectivenessReviewAdapter implements EffectivenessReviewPort {

    private final EffectivenessReviewJpaRepository repository;

    EffectivenessReviewAdapter(EffectivenessReviewJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(EffectivenessReview review) {
        repository.save(new EffectivenessReviewJpaEntity(
                review.id(), review.tenantId(), review.capaCaseId(), review.verdict(),
                review.criterion(), review.baseline(), review.target(), review.observed(),
                review.comment(), review.reviewerId(), review.reviewerName(),
                review.reviewedAt()));
    }

    @Override
    public List<EffectivenessReview> findByCase(UUID tenantId, UUID capaCaseId) {
        return repository.findByTenantIdAndCapaCaseIdOrderByReviewedAtDesc(tenantId, capaCaseId)
                .stream()
                .map(EffectivenessReviewAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<EffectivenessReview> findLastByCase(UUID tenantId, UUID capaCaseId) {
        // La liste est déjà triée du plus récent au plus ancien : prendre la
        // première évite une seconde requête, et le volume d'un historique de
        // dossier se compte en unités.
        return findByCase(tenantId, capaCaseId).stream().findFirst();
    }

    @Override
    public Map<UUID, EffectivenessReview> findLastPerCase(UUID tenantId) {
        Map<UUID, EffectivenessReview> last = new LinkedHashMap<>();
        for (EffectivenessReviewJpaEntity e : repository.findLastPerCase(tenantId)) {
            last.put(e.getCapaCaseId(), toDomain(e));
        }
        return last;
    }

    @Override
    public Map<UUID, Integer> countPerCase(UUID tenantId) {
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        for (Object[] row : repository.countPerCase(tenantId)) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    /**
     * Reconstitution SANS rejouer les règles de saisie.
     *
     * <p>Elles portent sur un geste d'utilisateur. Les appliquer à la relecture
     * ferait disparaître de l'historique une évaluation enregistrée avant qu'une
     * règle n'existe : un auditeur verrait un trou là où il y a une trace.
     */
    private static EffectivenessReview toDomain(EffectivenessReviewJpaEntity e) {
        return EffectivenessReview.rehydrate(e.getId(), e.getTenantId(), e.getCapaCaseId(),
                e.getVerdict(), e.getCriterion(), e.getBaseline(), e.getTarget(),
                e.getObserved(), e.getComment(), e.getReviewerId(), e.getReviewerName(),
                e.getReviewedAt());
    }
}
