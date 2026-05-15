package com.openlab.qualitos.quality.training;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Matrice de compétences : niveau actuel d'un utilisateur sur une skill donnée.
 * Le couple (tenant_id, user_id, skill_id) est UNIQUE — un seul état courant par
 * combinaison ; l'historique des évaluations passées passe par les enrollments
 * complétés et un évent log future (out-of-scope V1).
 */
@Entity
@Table(name = "training_user_skills",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_skill_unique",
                columnNames = {"tenant_id", "user_id", "skill_id"}),
        indexes = {
                @Index(name = "idx_user_skill_tenant_user",
                        columnList = "tenant_id, user_id"),
                @Index(name = "idx_user_skill_skill",
                        columnList = "skill_id, level")
        })
public class UserSkillAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    /** Niveau 0..4 ; redondant avec l'ordinal mais facilite le SQL. */
    @Column(nullable = false)
    private int level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CompetencySource source;

    @Column(name = "assessed_by")
    private UUID assessedBy;

    @Column(name = "assessed_at", nullable = false)
    private LocalDate assessedAt;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (source == null) source = CompetencySource.MANAGER;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public boolean isExpiredAt(LocalDate ref) {
        return expiresOn != null && expiresOn.isBefore(ref);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getSkillId() { return skillId; }
    public void setSkillId(UUID skillId) { this.skillId = skillId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public CompetencySource getSource() { return source; }
    public void setSource(CompetencySource source) { this.source = source; }
    public UUID getAssessedBy() { return assessedBy; }
    public void setAssessedBy(UUID assessedBy) { this.assessedBy = assessedBy; }
    public LocalDate getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDate assessedAt) { this.assessedAt = assessedAt; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
