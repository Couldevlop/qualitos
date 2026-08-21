package com.openlab.qualitos.quality.controlplan.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ControlPlanLineJpaRepository extends JpaRepository<ControlPlanLineJpaEntity, UUID> {

    List<ControlPlanLineJpaEntity> findByPlanIdAndTenantIdOrderBySequenceNoAsc(UUID planId, UUID tenantId);

    Optional<ControlPlanLineJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
