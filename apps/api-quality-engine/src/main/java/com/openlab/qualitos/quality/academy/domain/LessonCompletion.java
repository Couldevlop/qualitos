package com.openlab.qualitos.quality.academy.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace de la complétion d'une leçon par un apprenant (idempotente : une seule
 * ligne par couple {@code (enrollment, lesson)} grâce à la contrainte unique).
 */
@Entity
@Table(name = "academy_lesson_completions",
        uniqueConstraints = @UniqueConstraint(name = "uk_academy_lc_enrollment_lesson",
                columnNames = {"enrollment_id", "lesson_id"}),
        indexes = @Index(name = "idx_academy_lc_enrollment", columnList = "enrollment_id"))
public class LessonCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        if (completedAt == null) completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(UUID enrollmentId) { this.enrollmentId = enrollmentId; }
    public UUID getLessonId() { return lessonId; }
    public void setLessonId(UUID lessonId) { this.lessonId = lessonId; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
