package com.openlab.qualitos.quality.change;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Lien d'impact : un change request → une entité QualitOS impactée.
 * Le couple (change_id, target_type, target_id) est UNIQUE — pas de doublon.
 *
 * Pas de FK dure vers les tables cibles : le module Change ne doit pas dépendre
 * du schéma des autres modules. La validation d'existence reste applicative et
 * optionnelle (le manager peut référencer un asset externe sans correspondance
 * QualitOS via target_type=OTHER).
 */
@Entity
@Table(name = "change_impacts",
        uniqueConstraints = @UniqueConstraint(name = "uk_change_impact_unique",
                columnNames = {"change_id", "target_type", "target_id"}),
        indexes = @Index(name = "idx_change_impact_change", columnList = "change_id"))
public class ChangeImpact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "change_id", nullable = false)
    private UUID changeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 32)
    private ChangeImpactTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getChangeId() { return changeId; }
    public void setChangeId(UUID changeId) { this.changeId = changeId; }
    public ChangeImpactTargetType getTargetType() { return targetType; }
    public void setTargetType(ChangeImpactTargetType targetType) { this.targetType = targetType; }
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
