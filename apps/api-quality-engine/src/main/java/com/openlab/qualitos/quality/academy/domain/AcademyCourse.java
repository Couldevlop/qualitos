package com.openlab.qualitos.quality.academy.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Cours e-learning (parcours par rôle + secteur, §19.3).
 *
 * <p>Distinct de {@code TrainingPath} (matrice de compétences §4.7) : un cours
 * porte du <b>contenu</b> (modules → leçons → quiz) et octroie, à sa complétion
 * réussie, des points de gamification + un certificat signé/ancré.</p>
 *
 * <p>Multi-tenant strict : {@code tenantId} provient toujours du JWT (OWASP A01).</p>
 */
@Entity
@Table(name = "academy_courses",
        uniqueConstraints = @UniqueConstraint(name = "uk_academy_course_tenant_code",
                columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_academy_course_tenant", columnList = "tenant_id"),
                @Index(name = "idx_academy_course_role", columnList = "tenant_id, target_role"),
                @Index(name = "idx_academy_course_sector", columnList = "tenant_id, industry_sector")
        })
public class AcademyCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "target_role", length = 100)
    private String targetRole;

    @Column(name = "industry_sector", length = 100)
    private String industrySector;

    @Column(name = "passing_score", nullable = false)
    private int passingScore;

    @Column(name = "points_reward", nullable = false)
    private int pointsReward;

    @Column(name = "validity_months")
    private Integer validityMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CourseStatus status;

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
        if (status == null) status = CourseStatus.DRAFT;
        if (passingScore == 0) passingScore = 70;
        if (pointsReward == 0) pointsReward = 50;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public String getIndustrySector() { return industrySector; }
    public void setIndustrySector(String industrySector) { this.industrySector = industrySector; }
    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int passingScore) { this.passingScore = passingScore; }
    public int getPointsReward() { return pointsReward; }
    public void setPointsReward(int pointsReward) { this.pointsReward = pointsReward; }
    public Integer getValidityMonths() { return validityMonths; }
    public void setValidityMonths(Integer validityMonths) { this.validityMonths = validityMonths; }
    public CourseStatus getStatus() { return status; }
    public void setStatus(CourseStatus status) { this.status = status; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
