package com.openlab.qualitos.quality.cyberincidents.infrastructure;

import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentSeverity;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStatus;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nis2_cyber_incidents",
        indexes = {
                @Index(name = "idx_cyb_tenant", columnList = "tenant_id"),
                @Index(name = "idx_cyb_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_cyb_early_warning_deadline", columnList = "early_warning_deadline_at"),
                @Index(name = "idx_cyb_initial_assessment_deadline", columnList = "initial_assessment_deadline_at"),
                @Index(name = "idx_cyb_final_report_deadline", columnList = "final_report_deadline_at"),
                @Index(name = "uq_cyb_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class CyberIncidentJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "early_warning_deadline_at", nullable = false, updatable = false)
    private Instant earlyWarningDeadlineAt;

    @Column(name = "initial_assessment_deadline_at", nullable = false, updatable = false)
    private Instant initialAssessmentDeadlineAt;

    @Column(name = "final_report_deadline_at", nullable = false, updatable = false)
    private Instant finalReportDeadlineAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 32)
    private CyberIncidentType incidentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CyberIncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CyberIncidentStatus status;

    @Column(name = "estimated_affected_users", nullable = false)
    private long estimatedAffectedUsers;

    @Column(name = "affected_assets", length = 2000)
    private String affectedAssetsCsv;

    @Column(name = "affected_services", length = 2000)
    private String affectedServicesCsv;

    @Column(name = "linked_breach_id")
    private UUID linkedBreachId;

    @Column(name = "containment_measures", length = 4000)
    private String containmentMeasures;

    @Column(name = "impact_description", length = 4000)
    private String impactDescription;

    @Column(name = "early_warning_sent_at")
    private Instant earlyWarningSentAt;

    @Column(name = "early_warning_reference", length = 250)
    private String earlyWarningReference;

    @Column(name = "initial_assessment_sent_at")
    private Instant initialAssessmentSentAt;

    @Column(name = "initial_assessment_reference", length = 250)
    private String initialAssessmentReference;

    @Column(name = "final_report_sent_at")
    private Instant finalReportSentAt;

    @Column(name = "final_report_reference", length = 250)
    private String finalReportReference;

    @Column(name = "closure_notes", length = 4000)
    private String closureNotes;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "reported_by", nullable = false, updatable = false)
    private UUID reportedByUserId;

    @Column(name = "handled_by")
    private UUID handledByUserId;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public String getReference() { return reference; }
    public void setReference(String v) { this.reference = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant v) { this.detectedAt = v; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant v) { this.occurredAt = v; }
    public Instant getEarlyWarningDeadlineAt() { return earlyWarningDeadlineAt; }
    public void setEarlyWarningDeadlineAt(Instant v) { this.earlyWarningDeadlineAt = v; }
    public Instant getInitialAssessmentDeadlineAt() { return initialAssessmentDeadlineAt; }
    public void setInitialAssessmentDeadlineAt(Instant v) { this.initialAssessmentDeadlineAt = v; }
    public Instant getFinalReportDeadlineAt() { return finalReportDeadlineAt; }
    public void setFinalReportDeadlineAt(Instant v) { this.finalReportDeadlineAt = v; }
    public CyberIncidentType getIncidentType() { return incidentType; }
    public void setIncidentType(CyberIncidentType v) { this.incidentType = v; }
    public CyberIncidentSeverity getSeverity() { return severity; }
    public void setSeverity(CyberIncidentSeverity v) { this.severity = v; }
    public CyberIncidentStatus getStatus() { return status; }
    public void setStatus(CyberIncidentStatus v) { this.status = v; }
    public long getEstimatedAffectedUsers() { return estimatedAffectedUsers; }
    public void setEstimatedAffectedUsers(long v) { this.estimatedAffectedUsers = v; }
    public String getAffectedAssetsCsv() { return affectedAssetsCsv; }
    public void setAffectedAssetsCsv(String v) { this.affectedAssetsCsv = v; }
    public String getAffectedServicesCsv() { return affectedServicesCsv; }
    public void setAffectedServicesCsv(String v) { this.affectedServicesCsv = v; }
    public UUID getLinkedBreachId() { return linkedBreachId; }
    public void setLinkedBreachId(UUID v) { this.linkedBreachId = v; }
    public String getContainmentMeasures() { return containmentMeasures; }
    public void setContainmentMeasures(String v) { this.containmentMeasures = v; }
    public String getImpactDescription() { return impactDescription; }
    public void setImpactDescription(String v) { this.impactDescription = v; }
    public Instant getEarlyWarningSentAt() { return earlyWarningSentAt; }
    public void setEarlyWarningSentAt(Instant v) { this.earlyWarningSentAt = v; }
    public String getEarlyWarningReference() { return earlyWarningReference; }
    public void setEarlyWarningReference(String v) { this.earlyWarningReference = v; }
    public Instant getInitialAssessmentSentAt() { return initialAssessmentSentAt; }
    public void setInitialAssessmentSentAt(Instant v) { this.initialAssessmentSentAt = v; }
    public String getInitialAssessmentReference() { return initialAssessmentReference; }
    public void setInitialAssessmentReference(String v) { this.initialAssessmentReference = v; }
    public Instant getFinalReportSentAt() { return finalReportSentAt; }
    public void setFinalReportSentAt(Instant v) { this.finalReportSentAt = v; }
    public String getFinalReportReference() { return finalReportReference; }
    public void setFinalReportReference(String v) { this.finalReportReference = v; }
    public String getClosureNotes() { return closureNotes; }
    public void setClosureNotes(String v) { this.closureNotes = v; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String v) { this.rejectionReason = v; }
    public UUID getReportedByUserId() { return reportedByUserId; }
    public void setReportedByUserId(UUID v) { this.reportedByUserId = v; }
    public UUID getHandledByUserId() { return handledByUserId; }
    public void setHandledByUserId(UUID v) { this.handledByUserId = v; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant v) { this.closedAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
