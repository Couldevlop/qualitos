package com.openlab.qualitos.quality.academy.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Inscription d'un apprenant à un cours e-learning + progression.
 *
 * <p>Le {@code progressPct} reflète l'avancement (leçons complétées / total).
 * Le {@code finalScore} est la moyenne des quiz une fois le cours terminé.
 * Multi-tenant strict ; l'utilisateur est résolu depuis le JWT (OWASP A01).</p>
 */
@Entity
@Table(name = "academy_enrollments",
        uniqueConstraints = @UniqueConstraint(name = "uk_academy_enrollment_user_course",
                columnNames = {"tenant_id", "user_id", "course_id"}),
        indexes = {
                @Index(name = "idx_academy_enrollment_user", columnList = "tenant_id, user_id"),
                @Index(name = "idx_academy_enrollment_course", columnList = "tenant_id, course_id")
        })
public class AcademyEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AcademyEnrollmentStatus status;

    @Column(name = "progress_pct", nullable = false)
    private int progressPct;

    @Column(name = "final_score")
    private Integer finalScore;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

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
        if (enrolledAt == null) enrolledAt = now;
        if (status == null) status = AcademyEnrollmentStatus.ENROLLED;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }
    public AcademyEnrollmentStatus getStatus() { return status; }
    public void setStatus(AcademyEnrollmentStatus status) { this.status = status; }
    public int getProgressPct() { return progressPct; }
    public void setProgressPct(int progressPct) { this.progressPct = progressPct; }
    public Integer getFinalScore() { return finalScore; }
    public void setFinalScore(Integer finalScore) { this.finalScore = finalScore; }
    public Instant getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(Instant enrolledAt) { this.enrolledAt = enrolledAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
