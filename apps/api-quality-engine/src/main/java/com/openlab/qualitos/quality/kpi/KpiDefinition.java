package com.openlab.qualitos.quality.kpi;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Définition de KPI (§6.2). Catalogue par tenant : (tenant_id, code) UNIQUE.
 *
 * Sémantique des seuils (cf. {@link KpiEvaluator}) :
 *  - HIGHER_IS_BETTER : OK si value ≥ target, WARNING entre warning et target,
 *    CRITICAL ≤ critical, sinon WARNING.
 *  - LOWER_IS_BETTER : symétrique inverse.
 * Si target/warning/critical sont nulls, on tombe en UNKNOWN.
 */
@Entity
@Table(name = "kpi_definitions",
        uniqueConstraints = @UniqueConstraint(name = "uk_kpi_def_tenant_code",
                columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_kpi_def_tenant", columnList = "tenant_id"),
                @Index(name = "idx_kpi_def_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_kpi_def_tenant_category", columnList = "tenant_id, category")
        })
public class KpiDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 64)
    private String category;

    @Column(length = 32)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private KpiDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private KpiFrequency frequency;

    @Column(name = "target_value", precision = 24, scale = 6)
    private BigDecimal targetValue;

    @Column(name = "threshold_warning", precision = 24, scale = 6)
    private BigDecimal thresholdWarning;

    @Column(name = "threshold_critical", precision = 24, scale = 6)
    private BigDecimal thresholdCritical;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private KpiStatus status;

    /** Industries applicables, CSV (cf. Industry Packs §5). */
    @Column(name = "applicable_industries_csv", length = 1000)
    private String applicableIndustriesCsv;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = KpiStatus.DRAFT;
        if (frequency == null) frequency = KpiFrequency.MONTHLY;
        if (direction == null) direction = KpiDirection.HIGHER_IS_BETTER;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public KpiDirection getDirection() { return direction; }
    public void setDirection(KpiDirection direction) { this.direction = direction; }
    public KpiFrequency getFrequency() { return frequency; }
    public void setFrequency(KpiFrequency frequency) { this.frequency = frequency; }
    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }
    public BigDecimal getThresholdWarning() { return thresholdWarning; }
    public void setThresholdWarning(BigDecimal thresholdWarning) { this.thresholdWarning = thresholdWarning; }
    public BigDecimal getThresholdCritical() { return thresholdCritical; }
    public void setThresholdCritical(BigDecimal thresholdCritical) { this.thresholdCritical = thresholdCritical; }
    public KpiStatus getStatus() { return status; }
    public void setStatus(KpiStatus status) { this.status = status; }
    public String getApplicableIndustriesCsv() { return applicableIndustriesCsv; }
    public void setApplicableIndustriesCsv(String applicableIndustriesCsv) {
        this.applicableIndustriesCsv = applicableIndustriesCsv;
    }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
