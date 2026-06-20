package com.openlab.qualitos.quality.dashboards.timetravel.application;

import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TimeTravelDto {
    private TimeTravelDto() {}

    public record KpiSnapshotView(
            UUID kpiId,
            String code,
            String name,
            String unit,
            BigDecimal value,
            Instant measuredPeriodStart,
            boolean present) {

        public static KpiSnapshotView of(KpiAsOfSnapshot s) {
            return new KpiSnapshotView(s.getKpiId(), s.getCode(), s.getName(),
                    s.getUnit(), s.getValue(), s.getMeasuredPeriodStart(), s.isPresent());
        }
    }

    /**
     * Whole-dashboard as-of result.
     *
     * @param asOf        the resolved instant of the snapshot
     * @param empty       true when no KPI had any data at that date (UI empty state)
     * @param kpis        one entry per ACTIVE KPI
     */
    public record DashboardSnapshotView(
            Instant asOf,
            boolean empty,
            List<KpiSnapshotView> kpis) {}
}
