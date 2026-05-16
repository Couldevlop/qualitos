package com.openlab.qualitos.quality.aiactfria.infrastructure;

import com.openlab.qualitos.quality.aiactfria.domain.FriaStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_act_fria",
        indexes = {
                @Index(name = "idx_fria_tenant", columnList = "tenant_id"),
                @Index(name = "idx_fria_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_fria_tenant_system", columnList = "tenant_id, ai_system_id"),
                @Index(name = "uq_fria_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class FriaJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(name = "ai_system_id", nullable = false)
    private UUID aiSystemId;

    @Column(name = "process_description", nullable = false, length = 4000)
    private String processDescription;

    @Column(name = "deployment_duration_description", length = 4000)
    private String deploymentDurationDescription;

    @Column(name = "affected_persons_categories", nullable = false, length = 4000)
    private String affectedPersonsCategories;

    @Column(name = "specific_risks", nullable = false, length = 4000)
    private String specificRisks;

    @Column(name = "mitigation_measures", length = 4000)
    private String mitigationMeasures;

    @Column(name = "human_oversight_measures", length = 4000)
    private String humanOversightMeasures;

    @Column(name = "complaint_mechanism_description", length = 4000)
    private String complaintMechanismDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FriaStatus status;

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

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "archived_reason", length = 2000)
    private String archivedReason;

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
    public String getProcessDescription() { return processDescription; }
    public void setProcessDescription(String v) { this.processDescription = v; }
    public String getDeploymentDurationDescription() { return deploymentDurationDescription; }
    public void setDeploymentDurationDescription(String v) { this.deploymentDurationDescription = v; }
    public String getAffectedPersonsCategories() { return affectedPersonsCategories; }
    public void setAffectedPersonsCategories(String v) { this.affectedPersonsCategories = v; }
    public String getSpecificRisks() { return specificRisks; }
    public void setSpecificRisks(String v) { this.specificRisks = v; }
    public String getMitigationMeasures() { return mitigationMeasures; }
    public void setMitigationMeasures(String v) { this.mitigationMeasures = v; }
    public String getHumanOversightMeasures() { return humanOversightMeasures; }
    public void setHumanOversightMeasures(String v) { this.humanOversightMeasures = v; }
    public String getComplaintMechanismDescription() { return complaintMechanismDescription; }
    public void setComplaintMechanismDescription(String v) { this.complaintMechanismDescription = v; }
    public FriaStatus getStatus() { return status; }
    public void setStatus(FriaStatus v) { this.status = v; }
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
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant v) { this.effectiveTo = v; }
    public String getArchivedReason() { return archivedReason; }
    public void setArchivedReason(String v) { this.archivedReason = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
