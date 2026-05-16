package com.openlab.qualitos.quality.aipmm.infrastructure;

import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStatus;
import com.openlab.qualitos.quality.aipmm.domain.PmmReviewFrequency;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_act_pmm_plans",
        indexes = {
                @Index(name = "idx_pmm_tenant", columnList = "tenant_id"),
                @Index(name = "idx_pmm_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_pmm_tenant_system", columnList = "tenant_id, ai_system_id"),
                @Index(name = "uq_pmm_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class PmmPlanJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(name = "ai_system_id", nullable = false)
    private UUID aiSystemId;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(length = 4000)
    private String description;

    @Column(name = "metrics_monitored", length = 4000)
    private String metricsMonitored;

    @Column(name = "collection_method", length = 4000)
    private String collectionMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_frequency", length = 32)
    private PmmReviewFrequency reviewFrequency;

    @Column(name = "responsible_party_description", length = 4000)
    private String responsiblePartyDescription;

    @Column(name = "trigger_criteria", length = 4000)
    private String triggerCriteria;

    @Column(name = "qms_link_reference", length = 250)
    private String qmsLinkReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PmmPlanStatus status;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "last_reviewed_by")
    private UUID lastReviewedByUserId;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspension_reason", length = 2000)
    private String suspensionReason;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "closure_reason", length = 2000)
    private String closureReason;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public String getReference() { return reference; }
    public void setReference(String v) { this.reference = v; }
    public UUID getAiSystemId() { return aiSystemId; }
    public void setAiSystemId(UUID v) { this.aiSystemId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getMetricsMonitored() { return metricsMonitored; }
    public void setMetricsMonitored(String v) { this.metricsMonitored = v; }
    public String getCollectionMethod() { return collectionMethod; }
    public void setCollectionMethod(String v) { this.collectionMethod = v; }
    public PmmReviewFrequency getReviewFrequency() { return reviewFrequency; }
    public void setReviewFrequency(PmmReviewFrequency v) { this.reviewFrequency = v; }
    public String getResponsiblePartyDescription() { return responsiblePartyDescription; }
    public void setResponsiblePartyDescription(String v) { this.responsiblePartyDescription = v; }
    public String getTriggerCriteria() { return triggerCriteria; }
    public void setTriggerCriteria(String v) { this.triggerCriteria = v; }
    public String getQmsLinkReference() { return qmsLinkReference; }
    public void setQmsLinkReference(String v) { this.qmsLinkReference = v; }
    public PmmPlanStatus getStatus() { return status; }
    public void setStatus(PmmPlanStatus v) { this.status = v; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant v) { this.activatedAt = v; }
    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(Instant v) { this.lastReviewedAt = v; }
    public UUID getLastReviewedByUserId() { return lastReviewedByUserId; }
    public void setLastReviewedByUserId(UUID v) { this.lastReviewedByUserId = v; }
    public Instant getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(Instant v) { this.suspendedAt = v; }
    public String getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(String v) { this.suspensionReason = v; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant v) { this.effectiveTo = v; }
    public String getClosureReason() { return closureReason; }
    public void setClosureReason(String v) { this.closureReason = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
