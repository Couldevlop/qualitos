package com.openlab.qualitos.quality.dashboards.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DashboardLayoutJpaRepository extends JpaRepository<DashboardLayoutJpaEntity, UUID> {

    @Query("""
        SELECT d FROM DashboardLayoutJpaEntity d
        WHERE d.tenantId = :tenantId
          AND (d.userId = :userId OR d.shared = true)
        ORDER BY d.updatedAt DESC
        """)
    List<DashboardLayoutJpaEntity> findVisibleForUser(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId);

    boolean existsByTenantIdAndUserIdAndName(UUID tenantId, UUID userId, String name);
}
