package com.openlab.qualitos.quality.standards;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificationRoadmapStageRepository
        extends JpaRepository<CertificationRoadmapStage, UUID> {

    List<CertificationRoadmapStage> findByTenantStandardIdOrderByOrderIndexAsc(UUID tenantStandardId);

    Optional<CertificationRoadmapStage> findByIdAndTenantStandardId(UUID id, UUID tenantStandardId);

    boolean existsByTenantStandardId(UUID tenantStandardId);
}
