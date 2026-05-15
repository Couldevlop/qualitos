package com.openlab.qualitos.quality.change;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Approbation multi-niveaux : une ligne par (change, approver). Le niveau (level)
 * permet d'ordonner les approbateurs (manager direct < directeur < etc.) — non
 * imposé en V1 (toutes les approbations sont parallèles), mais le champ est là
 * pour brancher le flux séquentiel plus tard.
 */
@Entity
@Table(name = "change_approvals",
        uniqueConstraints = @UniqueConstraint(name = "uk_change_approval_unique",
                columnNames = {"change_id", "approver_user_id"}),
        indexes = {
                @Index(name = "idx_change_approval_change", columnList = "change_id"),
                @Index(name = "idx_change_approval_approver",
                        columnList = "tenant_id, approver_user_id, decision")
        })
public class ChangeApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "change_id", nullable = false)
    private UUID changeId;

    @Column(name = "approver_user_id", nullable = false)
    private UUID approverUserId;

    @Column(name = "approval_level", nullable = false)
    private int approvalLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalDecision decision;

    @Column(length = 1000)
    private String comment;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (decision == null) decision = ApprovalDecision.PENDING;
        if (approvalLevel == 0) approvalLevel = 1;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getChangeId() { return changeId; }
    public void setChangeId(UUID changeId) { this.changeId = changeId; }
    public UUID getApproverUserId() { return approverUserId; }
    public void setApproverUserId(UUID approverUserId) { this.approverUserId = approverUserId; }
    public int getApprovalLevel() { return approvalLevel; }
    public void setApprovalLevel(int approvalLevel) { this.approvalLevel = approvalLevel; }
    public ApprovalDecision getDecision() { return decision; }
    public void setDecision(ApprovalDecision decision) { this.decision = decision; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
