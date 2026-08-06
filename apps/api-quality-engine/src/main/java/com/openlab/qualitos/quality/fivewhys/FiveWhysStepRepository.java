package com.openlab.qualitos.quality.fivewhys;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FiveWhysStepRepository extends JpaRepository<FiveWhysStep, UUID> {

    Optional<FiveWhysStep> findByIdAndTenantId(UUID id, UUID tenantId);

    /** L'ordre EST le sens de la méthode : jamais de lecture non ordonnée. */
    List<FiveWhysStep> findByAnalysisIdAndTenantIdOrderByPositionAsc(UUID analysisId, UUID tenantId);

    long countByAnalysisIdAndTenantId(UUID analysisId, UUID tenantId);
}
