package com.openlab.qualitos.quality.training;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Progression de gamification d'un apprenant (CLAUDE.md §4.7 + §19.3).
 *
 * <p>Une ligne par couple {@code (tenant_id, user_id)} : c'est le score agrégé
 * de l'utilisateur (points cumulés, ceinture, badges, compteur de complétions).
 * Multi-tenant strict — toute lecture/écriture filtre par {@code tenant_id}
 * issu du JWT, jamais du body (OWASP A01).</p>
 *
 * <p>La ceinture et les badges sont des données <b>dérivées</b> : ils sont
 * recalculés depuis les points/complétions par des règles pures
 * ({@link BeltLevel}, {@link Badge}) à chaque {@code complete}. On les persiste
 * pour servir la lecture sans recalcul, mais ils restent reconstructibles.</p>
 */
@Entity
@Table(name = "training_learner_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_learner_progress_tenant_user",
                columnNames = {"tenant_id", "user_id"}),
        indexes = @Index(name = "idx_learner_progress_tenant_points",
                columnList = "tenant_id, points"))
public class LearnerProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Total de points accumulés sur les complétions réussies. */
    @Column(nullable = false)
    private int points;

    /** Nombre de complétions réussies (parcours/quiz). */
    @Column(name = "completed_count", nullable = false)
    private int completedCount;

    /** Meilleur score obtenu (0-100), null tant qu'aucune complétion. */
    @Column(name = "best_score")
    private Integer bestScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "belt_level", nullable = false, length = 16)
    private BeltLevel beltLevel;

    /** Badges obtenus, sérialisés en liste CSV via {@link BadgeSetConverter}. */
    @Convert(converter = BadgeSetConverter.class)
    @Column(name = "badges", length = 512, nullable = false)
    private Set<Badge> badges = EnumSet.noneOf(Badge.class);

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (beltLevel == null) beltLevel = BeltLevel.WHITE;
        if (badges == null) badges = EnumSet.noneOf(Badge.class);
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }
    public Integer getBestScore() { return bestScore; }
    public void setBestScore(Integer bestScore) { this.bestScore = bestScore; }
    public BeltLevel getBeltLevel() { return beltLevel; }
    public void setBeltLevel(BeltLevel beltLevel) { this.beltLevel = beltLevel; }
    public Set<Badge> getBadges() {
        return badges == null ? Collections.emptySet() : badges;
    }
    public void setBadges(Set<Badge> badges) {
        this.badges = badges == null ? EnumSet.noneOf(Badge.class) : EnumSet.copyOf(badges);
    }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
