package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quality_revision_requests",
        indexes = {
                @Index(name = "idx_revision_requests_product",
                        columnList = "tenant_id, product_id, status"),
                @Index(name = "idx_revision_requests_trigger",
                        columnList = "tenant_id, trigger_ref_id")
        })
public class RevisionRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;

    @Column(name = "trigger_ref_id", nullable = false)
    private UUID triggerRefId;

    @Column(name = "trigger_ref_label", nullable = false, length = 120)
    private String triggerRefLabel;

    @Column(nullable = false, length = 1000)
    private String rationale;

    /**
     * TEXT contenant du JSON, et non JSONB : les tests tournent sur H2 en mode
     * PostgreSQL, qui ne connaît pas JSONB. La sérialisation est explicite côté
     * service, jamais déléguée à Hibernate.
     */
    @Column(name = "proposed_change", nullable = false, columnDefinition = "TEXT")
    private String proposedChange;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_note", length = 1000)
    private String decisionNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String v) { this.targetType = v; }
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID v) { this.targetId = v; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String v) { this.triggerType = v; }
    public UUID getTriggerRefId() { return triggerRefId; }
    public void setTriggerRefId(UUID v) { this.triggerRefId = v; }
    public String getTriggerRefLabel() { return triggerRefLabel; }
    public void setTriggerRefLabel(String v) { this.triggerRefLabel = v; }
    public String getRationale() { return rationale; }
    public void setRationale(String v) { this.rationale = v; }
    public String getProposedChange() { return proposedChange; }
    public void setProposedChange(String v) { this.proposedChange = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public UUID getDecidedBy() { return decidedBy; }
    public void setDecidedBy(UUID v) { this.decidedBy = v; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant v) { this.decidedAt = v; }
    public String getDecisionNote() { return decisionNote; }
    public void setDecisionNote(String v) { this.decisionNote = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
