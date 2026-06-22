package com.openlab.qualitos.quality.dashboards.annotations.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DashboardAnnotationJpaRepository
        extends JpaRepository<DashboardAnnotationJpaEntity, UUID> {

    Optional<DashboardAnnotationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<DashboardAnnotationJpaEntity> findByTenantIdAndChartKeyOrderByCreatedAtDesc(
            UUID tenantId, String chartKey);
}
