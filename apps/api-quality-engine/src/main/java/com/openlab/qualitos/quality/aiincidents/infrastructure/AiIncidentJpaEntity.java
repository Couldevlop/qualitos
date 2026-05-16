package com.openlab.qualitos.quality.aiincidents.infrastructure;

import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentSeverity;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_act_incidents",
        indexes = {
                @Index(name = "idx_aii_tenant", columnList = "tenant_id"),
                @Index(name = "idx_aii_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_aii_tenant_severity", columnList = "tenant_id, severity"),
                @Index(name = "idx_aii_tenant_system", columnList = "tenant_id, ai_system_id"),
                @Index(name = "uq_aii_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class AiIncidentJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(name = "ai_system_id", nullable = false)
    private UUID aiSystemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AiIncidentSeverity severity;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(name = "affected_persons_description", length = 4000)
    private String affectedPersonsDescription;

    @Column(name = "immediate_actions_taken", length = 4000)
    private String immediateActionsTaken;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiIncidentStatus status;

    @Column(name = "investigation_started_at")
    private Instant investigationStartedAt;

    @Column(name = "investigation_lead_user_id")
    private UUID investigationLeadUserId;

    @Column(name = "root_cause_analysis", length = 4000)
    private String rootCauseAnalysis;

    @Column(name = "corrective_actions", length = 4000)
    private String correctiveActions;

    @Column(name = "notified_regulator_at")
    private Instant notifiedRegulatorAt;

    @Column(name = "regulator_reference", length = 250)
    private String regulatorReference;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    @Column(name = "dismissal_reason", length = 2000)
    private String dismissalReason;

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
    public String getReference() { return reference; }
    public void setReference(String v) { this.reference = v; }
    public UUID getAiSystemId() { return aiSystemId; }
    public void setAiSystemId(UUID v) { this.aiSystemId = v; }
    public AiIncidentSeverity getSeverity() { return severity; }
    public void setSeverity(AiIncidentSeverity v) { this.severity = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getAffectedPersonsDescription() { return affectedPersonsDescription; }
    public void setAffectedPersonsDescription(String v) { this.affectedPersonsDescription = v; }
    public String getImmediateActionsTaken() { return immediateActionsTaken; }
    public void setImmediateActionsTaken(String v) { this.immediateActionsTaken = v; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant v) { this.occurredAt = v; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant v) { this.detectedAt = v; }
    public AiIncidentStatus getStatus() { return status; }
    public void setStatus(AiIncidentStatus v) { this.status = v; }
    public Instant getInvestigationStartedAt() { return investigationStartedAt; }
    public void setInvestigationStartedAt(Instant v) { this.investigationStartedAt = v; }
    public UUID getInvestigationLeadUserId() { return investigationLeadUserId; }
    public void setInvestigationLeadUserId(UUID v) { this.investigationLeadUserId = v; }
    public String getRootCauseAnalysis() { return rootCauseAnalysis; }
    public void setRootCauseAnalysis(String v) { this.rootCauseAnalysis = v; }
    public String getCorrectiveActions() { return correctiveActions; }
    public void setCorrectiveActions(String v) { this.correctiveActions = v; }
    public Instant getNotifiedRegulatorAt() { return notifiedRegulatorAt; }
    public void setNotifiedRegulatorAt(Instant v) { this.notifiedRegulatorAt = v; }
    public String getRegulatorReference() { return regulatorReference; }
    public void setRegulatorReference(String v) { this.regulatorReference = v; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant v) { this.closedAt = v; }
    public Instant getDismissedAt() { return dismissedAt; }
    public void setDismissedAt(Instant v) { this.dismissedAt = v; }
    public String getDismissalReason() { return dismissalReason; }
    public void setDismissalReason(String v) { this.dismissalReason = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
