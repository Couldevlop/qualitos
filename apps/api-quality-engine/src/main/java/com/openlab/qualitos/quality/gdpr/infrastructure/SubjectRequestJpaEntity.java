package com.openlab.qualitos.quality.gdpr.infrastructure;

import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStatus;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gdpr_subject_requests",
        indexes = {
                @Index(name = "idx_dsr_tenant", columnList = "tenant_id"),
                @Index(name = "idx_dsr_tenant_subj",
                        columnList = "tenant_id, subject_identifier_hash"),
                @Index(name = "idx_dsr_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_dsr_deadline", columnList = "deadline_at")
        })
public class SubjectRequestJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubjectRequestType type;

    @Column(name = "subject_identifier_hash", nullable = false, length = 64)
    private String subjectIdentifierHash;

    @Column(name = "subject_identifier_label", length = 250)
    private String subjectIdentifierLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubjectRequestStatus status;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "deadline_at", nullable = false)
    private Instant deadlineAt;

    @Column(nullable = false)
    private boolean extended;

    @Column(name = "in_progress_at")
    private Instant inProgressAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "resolution_notes", length = 4000)
    private String resolutionNotes;

    @Column(name = "evidence_url", length = 1024)
    private String evidenceUrl;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedByUserId;

    @Column(name = "handled_by")
    private UUID handledByUserId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public SubjectRequestType getType() { return type; }
    public void setType(SubjectRequestType type) { this.type = type; }
    public String getSubjectIdentifierHash() { return subjectIdentifierHash; }
    public void setSubjectIdentifierHash(String v) { this.subjectIdentifierHash = v; }
    public String getSubjectIdentifierLabel() { return subjectIdentifierLabel; }
    public void setSubjectIdentifierLabel(String v) { this.subjectIdentifierLabel = v; }
    public SubjectRequestStatus getStatus() { return status; }
    public void setStatus(SubjectRequestStatus status) { this.status = status; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant v) { this.receivedAt = v; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant v) { this.deadlineAt = v; }
    public boolean isExtended() { return extended; }
    public void setExtended(boolean v) { this.extended = v; }
    public Instant getInProgressAt() { return inProgressAt; }
    public void setInProgressAt(Instant v) { this.inProgressAt = v; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant v) { this.completedAt = v; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String v) { this.rejectionReason = v; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String v) { this.resolutionNotes = v; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String v) { this.evidenceUrl = v; }
    public UUID getRequestedByUserId() { return requestedByUserId; }
    public void setRequestedByUserId(UUID v) { this.requestedByUserId = v; }
    public UUID getHandledByUserId() { return handledByUserId; }
    public void setHandledByUserId(UUID v) { this.handledByUserId = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
