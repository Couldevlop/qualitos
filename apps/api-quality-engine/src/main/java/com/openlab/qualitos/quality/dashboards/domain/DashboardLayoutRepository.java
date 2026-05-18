package com.openlab.qualitos.quality.dashboards.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port — domain depends on this; the JPA adapter lives in infrastructure.
 */
public interface DashboardLayoutRepository {

    DashboardLayout save(DashboardLayout layout);

    Optional<DashboardLayout> findById(UUID id);

    /** Visible = owned by user OR shared within tenant. */
    List<DashboardLayout> findVisibleForUser(UUID tenantId, UUID userId);

    boolean existsByTenantUserName(UUID tenantId, UUID userId, String name);

    void delete(UUID id);
}
