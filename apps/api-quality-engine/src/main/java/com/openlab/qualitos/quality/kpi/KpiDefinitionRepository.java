package com.openlab.qualitos.quality.kpi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KpiDefinitionRepository extends JpaRepository<KpiDefinition, UUID> {

    Optional<KpiDefinition> findByTenantIdAndCode(UUID tenantId, String code);

    Page<KpiDefinition> findByTenantId(UUID tenantId, Pageable pageable);

    Page<KpiDefinition> findByTenantIdAndStatus(UUID tenantId, KpiStatus status, Pageable pageable);

    Page<KpiDefinition> findByTenantIdAndCategory(UUID tenantId, String category, Pageable pageable);
}
