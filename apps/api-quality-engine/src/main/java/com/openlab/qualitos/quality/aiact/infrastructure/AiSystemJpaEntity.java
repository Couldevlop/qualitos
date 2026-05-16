package com.openlab.qualitos.quality.aiact.infrastructure;

import com.openlab.qualitos.quality.aiact.domain.AiRiskClassification;
import com.openlab.qualitos.quality.aiact.domain.AiSystemRole;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_act_systems",
        indexes = {
                @Index(name = "idx_ais_tenant", columnList = "tenant_id"),
                @Index(name = "idx_ais_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_ais_tenant_risk", columnList = "tenant_id, risk_classification"),
                @Index(name = "uq_ais_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class AiSystemJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(length = 4000)
    private String description;

    @Column(name = "provider_name", length = 250)
    private String providerName;

    @Column(name = "intended_purpose", nullable = false, length = 4000)
    private String intendedPurpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_classification", nullable = false, length = 32)
    private AiRiskClassification riskClassification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiSystemRole role;

    @Column(name = "general_purpose", nullable = false)
    private boolean generalPurpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiSystemStatus status;

    @Column(name = "conformity_assessment_evidence_url", length = 1024)
    private String conformityAssessmentEvidenceUrl;

    @Column(name = "ce_marking_number", length = 250)
    private String ceMarkingNumber;

    @Column(name = "human_oversight_description", length = 4000)
    private String humanOversightDescription;

    @Column(name = "transparency_measures", length = 4000)
    private String transparencyMeasures;

    @Column(name = "data_governance_notes", length = 4000)
    private String dataGovernanceNotes;

    @Column(name = "linked_dpia_id")
    private UUID linkedDpiaId;

    @Column(name = "linked_processing_activity_ids", length = 4000)
    private String linkedProcessingActivityIdsCsv;

    @Column(name = "linked_automated_decision_ids", length = 4000)
    private String linkedAutomatedDecisionIdsCsv;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "withdrawal_reason", length = 2000)
    private String withdrawalReason;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public String getReference() { return reference; }
    public void setReference(String v) { this.reference = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String v) { this.providerName = v; }
    public String getIntendedPurpose() { return intendedPurpose; }
    public void setIntendedPurpose(String v) { this.intendedPurpose = v; }
    public AiRiskClassification getRiskClassification() { return riskClassification; }
    public void setRiskClassification(AiRiskClassification v) { this.riskClassification = v; }
    public AiSystemRole getRole() { return role; }
    public void setRole(AiSystemRole v) { this.role = v; }
    public boolean isGeneralPurpose() { return generalPurpose; }
    public void setGeneralPurpose(boolean v) { this.generalPurpose = v; }
    public AiSystemStatus getStatus() { return status; }
    public void setStatus(AiSystemStatus v) { this.status = v; }
    public String getConformityAssessmentEvidenceUrl() { return conformityAssessmentEvidenceUrl; }
    public void setConformityAssessmentEvidenceUrl(String v) { this.conformityAssessmentEvidenceUrl = v; }
    public String getCeMarkingNumber() { return ceMarkingNumber; }
    public void setCeMarkingNumber(String v) { this.ceMarkingNumber = v; }
    public String getHumanOversightDescription() { return humanOversightDescription; }
    public void setHumanOversightDescription(String v) { this.humanOversightDescription = v; }
    public String getTransparencyMeasures() { return transparencyMeasures; }
    public void setTransparencyMeasures(String v) { this.transparencyMeasures = v; }
    public String getDataGovernanceNotes() { return dataGovernanceNotes; }
    public void setDataGovernanceNotes(String v) { this.dataGovernanceNotes = v; }
    public UUID getLinkedDpiaId() { return linkedDpiaId; }
    public void setLinkedDpiaId(UUID v) { this.linkedDpiaId = v; }
    public String getLinkedProcessingActivityIdsCsv() { return linkedProcessingActivityIdsCsv; }
    public void setLinkedProcessingActivityIdsCsv(String v) { this.linkedProcessingActivityIdsCsv = v; }
    public String getLinkedAutomatedDecisionIdsCsv() { return linkedAutomatedDecisionIdsCsv; }
    public void setLinkedAutomatedDecisionIdsCsv(String v) { this.linkedAutomatedDecisionIdsCsv = v; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant v) { this.effectiveFrom = v; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant v) { this.effectiveTo = v; }
    public String getWithdrawalReason() { return withdrawalReason; }
    public void setWithdrawalReason(String v) { this.withdrawalReason = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
