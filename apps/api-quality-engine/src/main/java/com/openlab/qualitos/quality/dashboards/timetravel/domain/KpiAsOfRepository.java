package com.openlab.qualitos.quality.dashboards.timetravel.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Port — read-only historical (as-of) access to KPI state for a tenant.
 * Implemented over {@code kpi_definitions} + {@code kpi_measurements}.
 * Always tenant-scoped (OWASP A01).
 */
public interface KpiAsOfRepository {

    /**
     * @param tenantId tenant from JWT
     * @param asOf     instant; each KPI resolves to its last measurement whose
     *                 period_start ≤ asOf (or an absent snapshot if none)
     * @return one snapshot per ACTIVE KPI of the tenant, ordered by code
     */
    List<KpiAsOfSnapshot> snapshotAsOf(UUID tenantId, Instant asOf);
}
