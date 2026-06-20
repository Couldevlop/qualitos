package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistance d'un document normatif généré (§8.8). Les sections sont stockées
 * en JSON via {@code @JdbcTypeCode(LONGVARCHAR)} sur une colonne TEXT — jamais
 * {@code @Lob String} (cf. consigne migration).
 */
@Entity
@Table(name = "standard_norm_documents",
        indexes = {
                @Index(name = "idx_snd_tenant", columnList = "tenant_id"),
                @Index(name = "idx_snd_tenant_status", columnList = "tenant_id, status")
        })
public class NormDocJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "standard_id", nullable = false)
    private UUID standardId;

    @Column(name = "standard_code", nullable = false, length = 100)
    private String standardCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NormDocKind kind;

    @Column(nullable = false, length = 500)
    private String title;

    /** Sections sérialisées en JSON (TEXT). */
    @Column(name = "sections_json", columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String sectionsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NormDocStatus status;

    @Column(name = "ai_provider", length = 100)
    private String aiProvider;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submitted_by")
    private UUID submittedByUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedByUserId;

    @Column(name = "approval_notes", length = 4000)
    private String approvalNotes;

    @Column(name = "human_signature", length = 512)
    private String humanSignature;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getStandardId() { return standardId; }
    public void setStandardId(UUID v) { this.standardId = v; }
    public String getStandardCode() { return standardCode; }
    public void setStandardCode(String v) { this.standardCode = v; }
    public NormDocKind getKind() { return kind; }
    public void setKind(NormDocKind v) { this.kind = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getSectionsJson() { return sectionsJson; }
    public void setSectionsJson(String v) { this.sectionsJson = v; }
    public NormDocStatus getStatus() { return status; }
    public void setStatus(NormDocStatus v) { this.status = v; }
    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String v) { this.aiProvider = v; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant v) { this.submittedAt = v; }
    public UUID getSubmittedByUserId() { return submittedByUserId; }
    public void setSubmittedByUserId(UUID v) { this.submittedByUserId = v; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant v) { this.approvedAt = v; }
    public UUID getApprovedByUserId() { return approvedByUserId; }
    public void setApprovedByUserId(UUID v) { this.approvedByUserId = v; }
    public String getApprovalNotes() { return approvalNotes; }
    public void setApprovalNotes(String v) { this.approvalNotes = v; }
    public String getHumanSignature() { return humanSignature; }
    public void setHumanSignature(String v) { this.humanSignature = v; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String v) { this.rejectionReason = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
