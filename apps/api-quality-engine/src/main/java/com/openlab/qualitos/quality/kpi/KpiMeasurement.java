package com.openlab.qualitos.quality.kpi;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mesure d'un KPI sur une période. Le couple (kpi_id, period_start) est UNIQUE :
 * on garantit une mesure par période et on évite les doublons quand plusieurs
 * sources veulent écrire la même valeur.
 */
@Entity
@Table(name = "kpi_measurements",
        uniqueConstraints = @UniqueConstraint(name = "uk_kpi_measure_period",
                columnNames = {"kpi_id", "period_start"}),
        indexes = {
                @Index(name = "idx_kpi_measure_tenant_kpi_period",
                        columnList = "tenant_id, kpi_id, period_start"),
                @Index(name = "idx_kpi_measure_tenant_period",
                        columnList = "tenant_id, period_start")
        })
public class KpiMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "kpi_id", nullable = false)
    private UUID kpiId;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal value;

    @Column(length = 32)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MeasurementSource source;

    @Column(name = "recorded_by_user_id")
    private UUID recordedByUserId;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (source == null) source = MeasurementSource.MANUAL;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getKpiId() { return kpiId; }
    public void setKpiId(UUID kpiId) { this.kpiId = kpiId; }
    public Instant getPeriodStart() { return periodStart; }
    public void setPeriodStart(Instant periodStart) { this.periodStart = periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Instant periodEnd) { this.periodEnd = periodEnd; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public MeasurementSource getSource() { return source; }
    public void setSource(MeasurementSource source) { this.source = source; }
    public UUID getRecordedByUserId() { return recordedByUserId; }
    public void setRecordedByUserId(UUID recordedByUserId) { this.recordedByUserId = recordedByUserId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
