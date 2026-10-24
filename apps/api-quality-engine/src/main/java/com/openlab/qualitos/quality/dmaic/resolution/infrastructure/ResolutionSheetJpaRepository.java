package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResolutionSheetJpaRepository extends JpaRepository<ResolutionSheetJpaEntity, UUID> {

    Optional<ResolutionSheetJpaEntity> findByTenantIdAndProjectId(UUID tenantId, UUID projectId);
}
