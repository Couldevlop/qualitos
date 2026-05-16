package com.openlab.qualitos.quality.nis2measures.infrastructure;

import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureCategory;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStatus;
import com.openlab.qualitos.quality.nis2measures.domain.ResidualRiskRating;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nis2_risk_measures",
        indexes = {
                @Index(name = "idx_nis2m_tenant", columnList = "tenant_id"),
                @Index(name = "idx_nis2m_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_nis2m_tenant_category", columnList = "tenant_id, category"),
                @Index(name = "idx_nis2m_next_review", columnList = "next_review_due_at"),
                @Index(name = "uq_nis2m_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class Nis2RiskMeasureJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Nis2MeasureCategory category;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Nis2MeasureStatus status;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "maturity_level", nullable = false)
    private int maturityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "residual_risk_rating", nullable = false, length = 32)
    private ResidualRiskRating residualRiskRating;

    @Column(name = "critical_risk_justification", length = 4000)
    private String criticalRiskJustification;

    @Column(name = "review_interval_days", nullable = false)
    private int reviewIntervalDays;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedByUserId;

    @Column(name = "next_review_due_at")
    private Instant nextReviewDueAt;

    @Column(name = "evidence_urls", length = 4000)
    private String evidenceUrlsCsv;

    @Column(name = "linked_processing_activity_ids", length = 4000)
    private String linkedProcessingActivityIdsCsv;

    @Column(name = "linked_processor_agreement_ids", length = 4000)
    private String linkedProcessorAgreementIdsCsv;

    @Column(length = 4000)
    private String notes;

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
    public String getReference() { return reference; }
    public void setReference(String v) { this.reference = v; }
    public Nis2MeasureCategory getCategory() { return category; }
    public void setCategory(Nis2MeasureCategory v) { this.category = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public Nis2MeasureStatus getStatus() { return status; }
    public void setStatus(Nis2MeasureStatus v) { this.status = v; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID v) { this.ownerUserId = v; }
    public int getMaturityLevel() { return maturityLevel; }
    public void setMaturityLevel(int v) { this.maturityLevel = v; }
    public ResidualRiskRating getResidualRiskRating() { return residualRiskRating; }
    public void setResidualRiskRating(ResidualRiskRating v) { this.residualRiskRating = v; }
    public String getCriticalRiskJustification() { return criticalRiskJustification; }
    public void setCriticalRiskJustification(String v) { this.criticalRiskJustification = v; }
    public int getReviewIntervalDays() { return reviewIntervalDays; }
    public void setReviewIntervalDays(int v) { this.reviewIntervalDays = v; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant v) { this.effectiveFrom = v; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant v) { this.effectiveTo = v; }
    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(Instant v) { this.lastReviewedAt = v; }
    public UUID getReviewedByUserId() { return reviewedByUserId; }
    public void setReviewedByUserId(UUID v) { this.reviewedByUserId = v; }
    public Instant getNextReviewDueAt() { return nextReviewDueAt; }
    public void setNextReviewDueAt(Instant v) { this.nextReviewDueAt = v; }
    public String getEvidenceUrlsCsv() { return evidenceUrlsCsv; }
    public void setEvidenceUrlsCsv(String v) { this.evidenceUrlsCsv = v; }
    public String getLinkedProcessingActivityIdsCsv() { return linkedProcessingActivityIdsCsv; }
    public void setLinkedProcessingActivityIdsCsv(String v) { this.linkedProcessingActivityIdsCsv = v; }
    public String getLinkedProcessorAgreementIdsCsv() { return linkedProcessorAgreementIdsCsv; }
    public void setLinkedProcessorAgreementIdsCsv(String v) { this.linkedProcessorAgreementIdsCsv = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
