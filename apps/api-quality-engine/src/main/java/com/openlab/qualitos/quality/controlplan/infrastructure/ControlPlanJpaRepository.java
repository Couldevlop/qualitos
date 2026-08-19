package com.openlab.qualitos.quality.controlplan.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ControlPlanJpaRepository extends JpaRepository<ControlPlanJpaEntity, UUID> {

    List<ControlPlanJpaEntity> findByTenantIdAndProductIdOrderByPhaseAscRevisionDesc(
            UUID tenantId, UUID productId);

    Optional<ControlPlanJpaEntity> findByTenantIdAndProductIdAndPhaseAndStatus(
            UUID tenantId, UUID productId, String phase, String status);

    Optional<ControlPlanJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
