package com.openlab.qualitos.quality.training;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Inscription d'un utilisateur à un parcours.
 *
 * Le {@link #certificateCode} est un UUID v4 généré au passage en COMPLETED.
 * À ce stade pas d'ancrage blockchain (cf. blockchain-service à venir) — la
 * vérification publique repose sur la combinaison code + base de données.
 * Le hash blockchain s'ajoutera comme colonne nullable sans toucher au reste.
 */
@Entity
@Table(name = "training_enrollments",
        uniqueConstraints = @UniqueConstraint(name = "uk_enrollment_user_path",
                columnNames = {"tenant_id", "user_id", "path_id"}),
        indexes = {
                @Index(name = "idx_enrollment_user",
                        columnList = "tenant_id, user_id"),
                @Index(name = "idx_enrollment_cert", columnList = "certificate_code")
        })
public class TrainingEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "path_id", nullable = false)
    private UUID pathId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EnrollmentStatus status;

    @Column(name = "progress_pct", nullable = false)
    private int progressPct;

    @Column(name = "final_score")
    private Integer finalScore;

    @Column(name = "enrolled_on", nullable = false)
    private LocalDate enrolledOn;

    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(name = "completed_on")
    private LocalDate completedOn;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    /** UUID public, partagé via QR code par exemple. UNIQUE — pas plus d'un certif par code. */
    @Column(name = "certificate_code", unique = true, length = 36)
    private String certificateCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = EnrollmentStatus.ENROLLED;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getPathId() { return pathId; }
    public void setPathId(UUID pathId) { this.pathId = pathId; }
    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }
    public int getProgressPct() { return progressPct; }
    public void setProgressPct(int progressPct) { this.progressPct = progressPct; }
    public Integer getFinalScore() { return finalScore; }
    public void setFinalScore(Integer finalScore) { this.finalScore = finalScore; }
    public LocalDate getEnrolledOn() { return enrolledOn; }
    public void setEnrolledOn(LocalDate enrolledOn) { this.enrolledOn = enrolledOn; }
    public LocalDate getStartedOn() { return startedOn; }
    public void setStartedOn(LocalDate startedOn) { this.startedOn = startedOn; }
    public LocalDate getCompletedOn() { return completedOn; }
    public void setCompletedOn(LocalDate completedOn) { this.completedOn = completedOn; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }
    public String getCertificateCode() { return certificateCode; }
    public void setCertificateCode(String certificateCode) { this.certificateCode = certificateCode; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
