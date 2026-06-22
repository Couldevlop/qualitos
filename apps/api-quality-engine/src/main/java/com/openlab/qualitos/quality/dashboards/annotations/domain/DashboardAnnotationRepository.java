package com.openlab.qualitos.quality.dashboards.annotations.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port — domain depends on this; the JPA adapter lives in infrastructure.
 * All reads are scoped by tenant to enforce isolation (OWASP A01).
 */
public interface DashboardAnnotationRepository {

    DashboardAnnotation save(DashboardAnnotation annotation);

    /** Tenant-scoped lookup; cross-tenant ids resolve to empty. */
    Optional<DashboardAnnotation> findByIdAndTenant(UUID id, UUID tenantId);

    /** Annotations on a chart for a tenant, newest first. */
    List<DashboardAnnotation> findByTenantAndChartKey(UUID tenantId, String chartKey);

    void delete(UUID id);
}
