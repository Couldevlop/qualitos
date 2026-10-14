package com.openlab.qualitos.quality.change;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * La carte d'un rôle de validation pour une demande.
 *
 * <p>Créée à la soumission, avec le LIBELLÉ du rôle recopié : renommer ou
 * retirer un rôle dans le paramétrage ne change ni les demandes en validation,
 * ni ce que l'historique raconte. Qui peut statuer se résout, lui, au moment de
 * la décision, sur les titulaires courants du rôle (absences, départs).
 */
@Entity
@Table(name = "change_validations",
        uniqueConstraints = @UniqueConstraint(name = "uq_change_validation_role", columnNames = {"change_id", "role_id"}),
        indexes = @Index(name = "ix_change_validations_role", columnList = "tenant_id, role_id, decision"))
public class ChangeValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "change_id", nullable = false)
    private UUID changeId;

    @Column(nullable = false)
    private int ordinal;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "role_label", nullable = false, length = 100)
    private String roleLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApprovalDecision decision;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_by_name", length = 200)
    private String decidedByName;

    @Column(length = 1000)
    private String comment;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /** Dernier rappel envoyé aux titulaires ; le délai suivant se compte depuis lui. */
    @Column(name = "last_reminded_at")
    private Instant lastRemindedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getChangeId() { return changeId; }
    public void setChangeId(UUID changeId) { this.changeId = changeId; }
    public int getOrdinal() { return ordinal; }
    public void setOrdinal(int ordinal) { this.ordinal = ordinal; }
    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }
    public String getRoleLabel() { return roleLabel; }
    public void setRoleLabel(String roleLabel) { this.roleLabel = roleLabel; }
    public ApprovalDecision getDecision() { return decision; }
    public void setDecision(ApprovalDecision decision) { this.decision = decision; }
    public UUID getDecidedBy() { return decidedBy; }
    public void setDecidedBy(UUID decidedBy) { this.decidedBy = decidedBy; }
    public String getDecidedByName() { return decidedByName; }
    public void setDecidedByName(String decidedByName) { this.decidedByName = decidedByName; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public Instant getLastRemindedAt() { return lastRemindedAt; }
    public void setLastRemindedAt(Instant lastRemindedAt) { this.lastRemindedAt = lastRemindedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
