package com.openlab.qualitos.quality.training;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Parcours de formation. Cible un rôle (ex "auditor", "iso-9001-pilot") et expose
 * un ensemble de skills + niveaux requis via {@link TrainingPathSkillRequirement}.
 */
@Entity
@Table(name = "training_paths",
        uniqueConstraints = @UniqueConstraint(name = "uk_training_path_tenant_code",
                columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_training_path_tenant", columnList = "tenant_id"),
                @Index(name = "idx_training_path_role", columnList = "tenant_id, target_role")
        })
public class TrainingPath {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "target_role", length = 100)
    private String targetRole;

    @Column(name = "duration_hours", nullable = false)
    private int durationHours;

    @Column(name = "passing_score", nullable = false)
    private int passingScore;

    @Column(name = "validity_months")
    private Integer validityMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TrainingPathStatus status;

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
        if (status == null) status = TrainingPathStatus.DRAFT;
        if (passingScore == 0) passingScore = 70;
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
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public int getDurationHours() { return durationHours; }
    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }
    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int passingScore) { this.passingScore = passingScore; }
    public Integer getValidityMonths() { return validityMonths; }
    public void setValidityMonths(Integer validityMonths) { this.validityMonths = validityMonths; }
    public TrainingPathStatus getStatus() { return status; }
    public void setStatus(TrainingPathStatus status) { this.status = status; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
