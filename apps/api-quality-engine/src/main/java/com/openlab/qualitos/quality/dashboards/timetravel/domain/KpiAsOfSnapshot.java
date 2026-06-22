package com.openlab.qualitos.quality.dashboards.timetravel.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable as-of snapshot of a single KPI: the last measurement whose period
 * had started on or before the requested instant (CLAUDE.md §7.3 "Time travel").
 *
 * <p>This is a pure value object. {@code value}/{@code measuredAt} are null when
 * the KPI had no measurement yet at the as-of date (empty-state handling).</p>
 */
public final class KpiAsOfSnapshot {

    private final UUID kpiId;
    private final String code;
    private final String name;
    private final String unit;
    private final BigDecimal value;
    private final Instant measuredPeriodStart;
    private final boolean present;

    public KpiAsOfSnapshot(UUID kpiId, String code, String name, String unit,
                           BigDecimal value, Instant measuredPeriodStart) {
        this.kpiId = Objects.requireNonNull(kpiId, "kpiId");
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.unit = unit;
        this.value = value;
        this.measuredPeriodStart = measuredPeriodStart;
        this.present = value != null;
    }

    public static KpiAsOfSnapshot withValue(UUID kpiId, String code, String name, String unit,
                                            BigDecimal value, Instant measuredPeriodStart) {
        if (value == null) {
            throw new IllegalArgumentException("value required for a present snapshot");
        }
        return new KpiAsOfSnapshot(kpiId, code, name, unit, value, measuredPeriodStart);
    }

    public static KpiAsOfSnapshot absent(UUID kpiId, String code, String name, String unit) {
        return new KpiAsOfSnapshot(kpiId, code, name, unit, null, null);
    }

    public UUID getKpiId() { return kpiId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public BigDecimal getValue() { return value; }
    public Instant getMeasuredPeriodStart() { return measuredPeriodStart; }
    public boolean isPresent() { return present; }
}
