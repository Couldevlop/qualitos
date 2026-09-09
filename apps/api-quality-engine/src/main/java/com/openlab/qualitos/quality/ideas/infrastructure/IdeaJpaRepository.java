package com.openlab.qualitos.quality.ideas.infrastructure;

import com.openlab.qualitos.quality.circle.CircleProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdeaJpaRepository extends JpaRepository<CircleProposal, UUID> {

    Optional<CircleProposal> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CircleProposal> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
