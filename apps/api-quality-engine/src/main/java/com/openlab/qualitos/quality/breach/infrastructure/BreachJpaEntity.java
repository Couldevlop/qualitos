package com.openlab.qualitos.quality.breach.infrastructure;

import com.openlab.qualitos.quality.breach.domain.BreachSeverity;
import com.openlab.qualitos.quality.breach.domain.BreachStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gdpr_breach_incidents",
        indexes = {
                @Index(name = "idx_breach_tenant", columnList = "tenant_id"),
                @Index(name = "idx_breach_tenant_status",
                        columnList = "tenant_id, status"),
                @Index(name = "idx_breach_dpa_deadline", columnList = "dpa_deadline_at"),
                @Index(name = "uq_breach_tenant_reference",
                        columnList = "tenant_id, internal_reference", unique = true)
        })
public class BreachJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "internal_reference", nullable = false, length = 64)
    private String internalReference;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "dpa_deadline_at", nullable = false, updatable = false)
    private Instant dpaDeadlineAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BreachSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BreachStatus status;

    @Column(name = "affected_subjects_count", nullable = false)
    private long affectedSubjectsCount;

    /** Stocké comme CSV (catégories au format kebab/dot strict, voir agrégat). */
    @Column(name = "affected_data_categories", length = 2000)
    private String affectedDataCategoriesCsv;

    @Column(name = "risk_of_harm_description", length = 2000)
    private String riskOfHarmDescription;

    @Column(name = "containment_measures", length = 4000)
    private String containmentMeasures;

    @Column(name = "dpa_notified_at")
    private Instant dpaNotifiedAt;

    @Column(name = "dpa_reference", length = 250)
    private String dpaReference;

    @Column(name = "subjects_notified_at")
    private Instant subjectsNotifiedAt;

    @Column(name = "subjects_notification_channel", length = 250)
    private String subjectsNotificationChannel;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "closure_notes", length = 4000)
    private String closureNotes;

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
    public String getInternalReference() { return internalReference; }
    public void setInternalReference(String v) { this.internalReference = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant v) { this.detectedAt = v; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant v) { this.occurredAt = v; }
    public Instant getDpaDeadlineAt() { return dpaDeadlineAt; }
    public void setDpaDeadlineAt(Instant v) { this.dpaDeadlineAt = v; }
    public BreachSeverity getSeverity() { return severity; }
    public void setSeverity(BreachSeverity v) { this.severity = v; }
    public BreachStatus getStatus() { return status; }
    public void setStatus(BreachStatus v) { this.status = v; }
    public long getAffectedSubjectsCount() { return affectedSubjectsCount; }
    public void setAffectedSubjectsCount(long v) { this.affectedSubjectsCount = v; }
    public String getAffectedDataCategoriesCsv() { return affectedDataCategoriesCsv; }
    public void setAffectedDataCategoriesCsv(String v) { this.affectedDataCategoriesCsv = v; }
    public String getRiskOfHarmDescription() { return riskOfHarmDescription; }
    public void setRiskOfHarmDescription(String v) { this.riskOfHarmDescription = v; }
    public String getContainmentMeasures() { return containmentMeasures; }
    public void setContainmentMeasures(String v) { this.containmentMeasures = v; }
    public Instant getDpaNotifiedAt() { return dpaNotifiedAt; }
    public void setDpaNotifiedAt(Instant v) { this.dpaNotifiedAt = v; }
    public String getDpaReference() { return dpaReference; }
    public void setDpaReference(String v) { this.dpaReference = v; }
    public Instant getSubjectsNotifiedAt() { return subjectsNotifiedAt; }
    public void setSubjectsNotifiedAt(Instant v) { this.subjectsNotifiedAt = v; }
    public String getSubjectsNotificationChannel() { return subjectsNotificationChannel; }
    public void setSubjectsNotificationChannel(String v) { this.subjectsNotificationChannel = v; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String v) { this.rejectionReason = v; }
    public String getClosureNotes() { return closureNotes; }
    public void setClosureNotes(String v) { this.closureNotes = v; }
    public UUID getReportedByUserId() { return reportedByUserId; }
    public void setReportedByUserId(UUID v) { this.reportedByUserId = v; }
    public UUID getHandledByUserId() { return handledByUserId; }
    public void setHandledByUserId(UUID v) { this.handledByUserId = v; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant v) { this.closedAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
