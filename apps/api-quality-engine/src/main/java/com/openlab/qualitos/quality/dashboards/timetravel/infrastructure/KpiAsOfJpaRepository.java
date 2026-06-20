package com.openlab.qualitos.quality.dashboards.timetravel.infrastructure;

import com.openlab.qualitos.quality.kpi.KpiDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * As-of read model over {@code kpi_definitions} LEFT JOINed to the latest
 * {@code kpi_measurements} row whose period_start ≤ :asOf, for ACTIVE KPIs of
 * a tenant. Native SQL keeps the correlated "latest measurement" subquery clear
 * and portable to PostgreSQL.
 *
 * <p>Bound to {@link KpiDefinition} only to satisfy Spring Data's typed
 * repository contract; the actual rows are returned via the projection.</p>
 */
public interface KpiAsOfJpaRepository extends JpaRepository<KpiDefinition, UUID> {

    @Query(value = """
            SELECT d.id            AS kpiId,
                   d.code          AS code,
                   d.name          AS name,
                   d.unit          AS unit,
                   m.value         AS value,
                   m.period_start  AS measuredPeriodStart
            FROM kpi_definitions d
            LEFT JOIN LATERAL (
                SELECT km.value, km.period_start
                FROM kpi_measurements km
                WHERE km.kpi_id = d.id
                  AND km.tenant_id = d.tenant_id
                  AND km.period_start <= :asOf
                ORDER BY km.period_start DESC
                LIMIT 1
            ) m ON TRUE
            WHERE d.tenant_id = :tenantId
              AND d.status = 'ACTIVE'
            ORDER BY d.code ASC
            """, nativeQuery = true)
    List<KpiAsOfRow> snapshotAsOf(@Param("tenantId") UUID tenantId,
                                  @Param("asOf") Instant asOf);
}
