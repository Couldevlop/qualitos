package com.openlab.qualitos.quality.retention.infrastructure;

import com.openlab.qualitos.quality.retention.domain.RetentionRuleStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gdpr_retention_rules",
        indexes = {
                @Index(name = "idx_retention_tenant", columnList = "tenant_id"),
                @Index(name = "idx_retention_tenant_category",
                        columnList = "tenant_id, data_category_code"),
                @Index(name = "idx_retention_tenant_status",
                        columnList = "tenant_id, status")
        })
public class RetentionRuleJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "data_category_code", nullable = false, length = 64)
    private String dataCategoryCode;

    @Column(name = "data_category_label", length = 250)
    private String dataCategoryLabel;

    /** Stocké en secondes — Duration.ofSeconds(...) au mapping. */
    @Column(name = "retention_period_seconds", nullable = false)
    private long retentionPeriodSeconds;

    @Column(name = "legal_basis", nullable = false, length = 2000)
    private String legalBasis;

    @Column(name = "lawful_basis_reference", length = 1024)
    private String lawfulBasisReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RetentionRuleStatus status;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public String getDataCategoryCode() { return dataCategoryCode; }
    public void setDataCategoryCode(String v) { this.dataCategoryCode = v; }
    public String getDataCategoryLabel() { return dataCategoryLabel; }
    public void setDataCategoryLabel(String v) { this.dataCategoryLabel = v; }
    public long getRetentionPeriodSeconds() { return retentionPeriodSeconds; }
    public void setRetentionPeriodSeconds(long v) { this.retentionPeriodSeconds = v; }
    public String getLegalBasis() { return legalBasis; }
    public void setLegalBasis(String v) { this.legalBasis = v; }
    public String getLawfulBasisReference() { return lawfulBasisReference; }
    public void setLawfulBasisReference(String v) { this.lawfulBasisReference = v; }
    public RetentionRuleStatus getStatus() { return status; }
    public void setStatus(RetentionRuleStatus v) { this.status = v; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant v) { this.effectiveFrom = v; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant v) { this.effectiveTo = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
