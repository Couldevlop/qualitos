package com.openlab.qualitos.quality.aiqms.infrastructure;

import com.openlab.qualitos.quality.aiqms.domain.AiQmsStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_act_qms",
        indexes = {
                @Index(name = "idx_aqms_tenant", columnList = "tenant_id"),
                @Index(name = "idx_aqms_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "uq_aqms_tenant_ref_version",
                        columnList = "tenant_id, reference, version", unique = true)
        })
public class AiQmsJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(length = 4000)
    private String description;

    @Column(name = "regulatory_compliance_strategy", length = 8000)
    private String regulatoryComplianceStrategy;

    @Column(name = "design_control_description", length = 8000)
    private String designControlDescription;

    @Column(name = "quality_control_description", length = 8000)
    private String qualityControlDescription;

    @Column(name = "data_management_description", length = 8000)
    private String dataManagementDescription;

    @Column(name = "risk_management_description", length = 8000)
    private String riskManagementDescription;

    @Column(name = "pmm_description", length = 8000)
    private String pmmDescription;

    @Column(name = "regulator_communication_description", length = 8000)
    private String regulatorCommunicationDescription;

    @Column(name = "resource_management_description", length = 8000)
    private String resourceManagementDescription;

    @Column(name = "supplier_monitoring_description", length = 8000)
    private String supplierMonitoringDescription;

    @Column(name = "covered_ai_system_ids", length = 8000)
    private String coveredAiSystemIdsCsv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiQmsStatus status;

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

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "superseded_by_qms_id")
    private UUID supersededByQmsId;

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
    public String getVersion() { return version; }
    public void setVersion(String v) { this.version = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getRegulatoryComplianceStrategy() { return regulatoryComplianceStrategy; }
    public void setRegulatoryComplianceStrategy(String v) { this.regulatoryComplianceStrategy = v; }
    public String getDesignControlDescription() { return designControlDescription; }
    public void setDesignControlDescription(String v) { this.designControlDescription = v; }
    public String getQualityControlDescription() { return qualityControlDescription; }
    public void setQualityControlDescription(String v) { this.qualityControlDescription = v; }
    public String getDataManagementDescription() { return dataManagementDescription; }
    public void setDataManagementDescription(String v) { this.dataManagementDescription = v; }
    public String getRiskManagementDescription() { return riskManagementDescription; }
    public void setRiskManagementDescription(String v) { this.riskManagementDescription = v; }
    public String getPmmDescription() { return pmmDescription; }
    public void setPmmDescription(String v) { this.pmmDescription = v; }
    public String getRegulatorCommunicationDescription() { return regulatorCommunicationDescription; }
    public void setRegulatorCommunicationDescription(String v) { this.regulatorCommunicationDescription = v; }
    public String getResourceManagementDescription() { return resourceManagementDescription; }
    public void setResourceManagementDescription(String v) { this.resourceManagementDescription = v; }
    public String getSupplierMonitoringDescription() { return supplierMonitoringDescription; }
    public void setSupplierMonitoringDescription(String v) { this.supplierMonitoringDescription = v; }
    public String getCoveredAiSystemIdsCsv() { return coveredAiSystemIdsCsv; }
    public void setCoveredAiSystemIdsCsv(String v) { this.coveredAiSystemIdsCsv = v; }
    public AiQmsStatus getStatus() { return status; }
    public void setStatus(AiQmsStatus v) { this.status = v; }
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
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant v) { this.effectiveFrom = v; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant v) { this.effectiveTo = v; }
    public UUID getSupersededByQmsId() { return supersededByQmsId; }
    public void setSupersededByQmsId(UUID v) { this.supersededByQmsId = v; }
    public String getArchivedReason() { return archivedReason; }
    public void setArchivedReason(String v) { this.archivedReason = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
