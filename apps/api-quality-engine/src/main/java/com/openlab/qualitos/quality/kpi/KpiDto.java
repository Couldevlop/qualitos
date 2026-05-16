package com.openlab.qualitos.quality.kpi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class KpiDto {

    private KpiDto() {}

    // ---- Definition ----

    public record CreateKpiRequest(
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "^[a-z0-9][a-z0-9_-]{1,99}$",
                    message = "code must be lowercase kebab/snake (2..100 chars)")
            String code,
            @NotBlank @Size(max = 250) String name,
            @Size(max = 2000) String description,
            @Size(max = 64) String category,
            @Size(max = 32) String unit,
            @NotNull KpiDirection direction,
            KpiFrequency frequency,
            BigDecimal targetValue,
            BigDecimal thresholdWarning,
            BigDecimal thresholdCritical,
            @Size(max = 1000) String applicableIndustriesCsv,
            UUID ownerUserId,
            @NotNull UUID createdBy
    ) {}

    public record UpdateKpiRequest(
            @Size(max = 250) String name,
            @Size(max = 2000) String description,
            @Size(max = 64) String category,
            @Size(max = 32) String unit,
            KpiFrequency frequency,
            BigDecimal targetValue,
            BigDecimal thresholdWarning,
            BigDecimal thresholdCritical,
            @Size(max = 1000) String applicableIndustriesCsv,
            UUID ownerUserId
    ) {}

    public record KpiResponse(
            UUID id, UUID tenantId, String code, String name, String description,
            String category, String unit,
            KpiDirection direction, KpiFrequency frequency,
            BigDecimal targetValue, BigDecimal thresholdWarning, BigDecimal thresholdCritical,
            KpiStatus status, String applicableIndustriesCsv,
            UUID ownerUserId, UUID createdBy,
            Instant createdAt, Instant updatedAt
    ) {}

    // ---- Measurement ----

    public record RecordMeasurementRequest(
            @NotNull Instant periodStart,
            @NotNull Instant periodEnd,
            @NotNull BigDecimal value,
            @Size(max = 32) String unit,
            MeasurementSource source,
            UUID recordedByUserId,
            @Size(max = 1000) String notes
    ) {}

    public record MeasurementResponse(
            UUID id, UUID tenantId, UUID kpiId,
            Instant periodStart, Instant periodEnd,
            BigDecimal value, String unit,
            MeasurementSource source, UUID recordedByUserId,
            String notes,
            KpiHealth health,
            Instant createdAt
    ) {}

    // ---- Status / trend ----

    public record KpiCurrentStatus(
            UUID kpiId,
            String code,
            String name,
            KpiStatus definitionStatus,
            KpiDirection direction,
            BigDecimal latestValue,
            String unit,
            Instant latestPeriodStart,
            Instant latestPeriodEnd,
            KpiHealth health,
            BigDecimal targetValue,
            BigDecimal thresholdWarning,
            BigDecimal thresholdCritical
    ) {}

    public record KpiTrendPoint(
            Instant periodStart,
            Instant periodEnd,
            BigDecimal value,
            KpiHealth health
    ) {}

    public record KpiTrend(
            UUID kpiId,
            String code,
            int sampleCount,
            List<KpiTrendPoint> points
    ) {}
}
