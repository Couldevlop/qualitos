package com.openlab.qualitos.quality.dashboards.timetravel.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Projection of the as-of query: one ACTIVE KPI definition joined to its last
 * measurement at-or-before the as-of instant (measurement columns null when none).
 */
public interface KpiAsOfRow {
    UUID getKpiId();
    String getCode();
    String getName();
    String getUnit();
    BigDecimal getValue();
    Instant getMeasuredPeriodStart();
}
