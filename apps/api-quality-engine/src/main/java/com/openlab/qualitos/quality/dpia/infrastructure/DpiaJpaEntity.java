package com.openlab.qualitos.quality.dpia.infrastructure;

import com.openlab.qualitos.quality.dpia.domain.DpiaStatus;
import com.openlab.qualitos.quality.dpia.domain.RiskLevel;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gdpr_dpias",
        indexes = {
                @Index(name = "idx_dpia_tenant", columnList = "tenant_id"),
                @Index(name = "idx_dpia_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "uq_dpia_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class DpiaJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 4000)
    private String description;

    /** CSV d'UUID des activités RoPA liées. */
    @Column(name = "linked_processing_activity_ids", length = 4000)
    private String linkedProcessingActivityIdsCsv;

    @Column(name = "necessity_notes", length = 8000)
    private String necessityAndProportionalityNotes;

    @Column(name = "risks_to_rights", length = 8000)
    private String risksToRightsAndFreedoms;

    @Column(name = "mitigation_measures", length = 8000)
    private String mitigationMeasures;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_risk_level", nullable = false, length = 32)
    private RiskLevel overallRiskLevel;

    @Column(name = "consultation_required", nullable = false)
    private boolean consultationRequired;

    @Column(name = "consultation_notes", length = 8000)
    private String consultationNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DpiaStatus status;

    @Column(name = "dpo_user_id")
    private UUID dpoUserId;

    @Column(name = "dpo_opinion", length = 8000)
    private String dpoOpinion;

    @Column(name = "dpo_opinion_at")
    private Instant dpoOpinionAt;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "handled_by")
    private UUID handledByUserId;

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
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getLinkedProcessingActivityIdsCsv() { return linkedProcessingActivityIdsCsv; }
    public void setLinkedProcessingActivityIdsCsv(String v) { this.linkedProcessingActivityIdsCsv = v; }
    public String getNecessityAndProportionalityNotes() { return necessityAndProportionalityNotes; }
    public void setNecessityAndProportionalityNotes(String v) { this.necessityAndProportionalityNotes = v; }
    public String getRisksToRightsAndFreedoms() { return risksToRightsAndFreedoms; }
    public void setRisksToRightsAndFreedoms(String v) { this.risksToRightsAndFreedoms = v; }
    public String getMitigationMeasures() { return mitigationMeasures; }
    public void setMitigationMeasures(String v) { this.mitigationMeasures = v; }
    public RiskLevel getOverallRiskLevel() { return overallRiskLevel; }
    public void setOverallRiskLevel(RiskLevel v) { this.overallRiskLevel = v; }
    public boolean isConsultationRequired() { return consultationRequired; }
    public void setConsultationRequired(boolean v) { this.consultationRequired = v; }
    public String getConsultationNotes() { return consultationNotes; }
    public void setConsultationNotes(String v) { this.consultationNotes = v; }
    public DpiaStatus getStatus() { return status; }
    public void setStatus(DpiaStatus v) { this.status = v; }
    public UUID getDpoUserId() { return dpoUserId; }
    public void setDpoUserId(UUID v) { this.dpoUserId = v; }
    public String getDpoOpinion() { return dpoOpinion; }
    public void setDpoOpinion(String v) { this.dpoOpinion = v; }
    public Instant getDpoOpinionAt() { return dpoOpinionAt; }
    public void setDpoOpinionAt(Instant v) { this.dpoOpinionAt = v; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant v) { this.effectiveFrom = v; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant v) { this.effectiveTo = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public UUID getHandledByUserId() { return handledByUserId; }
    public void setHandledByUserId(UUID v) { this.handledByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
