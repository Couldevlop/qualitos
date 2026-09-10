package com.openlab.qualitos.quality.ideas.infrastructure;

import com.openlab.qualitos.quality.circle.CircleProposal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdeaJpaRepository extends JpaRepository<CircleProposal, UUID> {

    /**
     * {@code circle} est {@code LAZY} et {@code open-in-view} est désactivé :
     * sans ce graphe, {@code IdeaMapper} lirait l'identifiant du cercle hors
     * session dès que l'appelant n'est pas transactionnel (voir
     * {@link IdeaRepositoryAdapter}).
     */
    @EntityGraph(attributePaths = "circle")
    Optional<CircleProposal> findByIdAndTenantId(UUID id, UUID tenantId);

    @EntityGraph(attributePaths = "circle")
    List<CircleProposal> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
