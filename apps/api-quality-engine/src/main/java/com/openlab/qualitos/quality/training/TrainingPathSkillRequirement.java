package com.openlab.qualitos.quality.training;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Skill exigée par un parcours, avec niveau cible.
 * Table d'association explicite (et pas @ManyToMany) parce qu'on porte un attribut
 * de relation (level) — pattern recommandé pour éviter une jointure cachée.
 */
@Entity
@Table(name = "training_path_skill_requirements",
        uniqueConstraints = @UniqueConstraint(name = "uk_tpsr_path_skill",
                columnNames = {"path_id", "skill_id"}),
        indexes = @Index(name = "idx_tpsr_path", columnList = "path_id"))
public class TrainingPathSkillRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "path_id", nullable = false)
    private UUID pathId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "target_level", nullable = false)
    private int targetLevel;

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
    public UUID getPathId() { return pathId; }
    public void setPathId(UUID pathId) { this.pathId = pathId; }
    public UUID getSkillId() { return skillId; }
    public void setSkillId(UUID skillId) { this.skillId = skillId; }
    public int getTargetLevel() { return targetLevel; }
    public void setTargetLevel(int targetLevel) { this.targetLevel = targetLevel; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
