package com.openlab.qualitos.quality.academy.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Certificat de complétion de cours, signé ML-DSA (hybride Ed25519 + ML-DSA-65)
 * et ancré blockchain, vérifiable publiquement par QR code (§19.3).
 *
 * <p>Le {@code code} est l'identifiant public porté par le QR : l'autorité de
 * vérification est le code lui-même (pas de filtre tenant à la lecture publique).
 * La {@code signature} est l'enveloppe encodée Base64URL ; {@code anchorTxRef}
 * la référence d'ancrage. Réutilise l'infra crypto/ancrage du Standards Hub
 * (aucune crypto réimplémentée).</p>
 */
@Entity
@Table(name = "academy_certificates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_academy_cert_code", columnNames = {"code"}),
                @UniqueConstraint(name = "uk_academy_cert_enrollment", columnNames = {"enrollment_id"})
        },
        indexes = {
                @Index(name = "idx_academy_cert_code", columnList = "code"),
                @Index(name = "idx_academy_cert_user", columnList = "tenant_id, user_id")
        })
public class AcademyCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "course_code", nullable = false, length = 100)
    private String courseCode;

    @Column(name = "course_title", nullable = false, length = 200)
    private String courseTitle;

    @Column(name = "final_score", nullable = false)
    private int finalScore;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String signature;

    @Column(name = "anchor_tx_ref", length = 200)
    private String anchorTxRef;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (issuedAt == null) issuedAt = now;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(UUID enrollmentId) { this.enrollmentId = enrollmentId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public int getFinalScore() { return finalScore; }
    public void setFinalScore(int finalScore) { this.finalScore = finalScore; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getAnchorTxRef() { return anchorTxRef; }
    public void setAnchorTxRef(String anchorTxRef) { this.anchorTxRef = anchorTxRef; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
