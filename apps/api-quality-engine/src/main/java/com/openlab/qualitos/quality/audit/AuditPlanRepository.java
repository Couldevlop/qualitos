package com.openlab.qualitos.quality.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditPlanRepository extends JpaRepository<AuditPlan, UUID> {

    Page<AuditPlan> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AuditPlan> findByTenantIdAndStatus(UUID tenantId, AuditStatus status, Pageable pageable);

    Page<AuditPlan> findByTenantIdAndType(UUID tenantId, AuditType type, Pageable pageable);

    Optional<AuditPlan> findByIdAndTenantId(UUID id, UUID tenantId);
}
