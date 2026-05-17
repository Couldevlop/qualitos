package com.openlab.qualitos.quality.aieudb.infrastructure;

import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_act_eudb_registrations",
        indexes = {
                @Index(name = "idx_eudb_tenant", columnList = "tenant_id"),
                @Index(name = "idx_eudb_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_eudb_tenant_system", columnList = "tenant_id, ai_system_id"),
                @Index(name = "uq_eudb_tenant_reference",
                        columnList = "tenant_id, reference", unique = true),
                @Index(name = "uq_eudb_tenant_eudbid",
                        columnList = "tenant_id, eudb_id", unique = true)
        })
public class EudbRegistrationJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(name = "ai_system_id", nullable = false)
    private UUID aiSystemId;

    @Column(name = "provider_entity_name", length = 250)
    private String providerEntityName;

    @Column(name = "provider_eu_representative", length = 250)
    private String providerEuRepresentative;

    @Column(name = "member_state_of_reference", length = 2)
    private String memberStateOfReference;

    @Column(name = "intended_purpose_summary", length = 4000)
    private String intendedPurposeSummary;

    @Column(name = "technical_documentation_reference", length = 250)
    private String technicalDocumentationReference;

    @Column(name = "eudb_id", length = 64)
    private String eudbId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EudbRegistrationStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "submitted_by")
    private UUID submittedByUserId;

    @Column(name = "registration_date")
    private Instant registrationDate;

    @Column(name = "last_update_date")
    private Instant lastUpdateDate;

    @Column(name = "last_update_summary", length = 4000)
    private String lastUpdateSummary;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Column(name = "retirement_reason", length = 2000)
    private String retirementReason;

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
    public String getProviderEntityName() { return providerEntityName; }
    public void setProviderEntityName(String v) { this.providerEntityName = v; }
    public String getProviderEuRepresentative() { return providerEuRepresentative; }
    public void setProviderEuRepresentative(String v) { this.providerEuRepresentative = v; }
    public String getMemberStateOfReference() { return memberStateOfReference; }
    public void setMemberStateOfReference(String v) { this.memberStateOfReference = v; }
    public String getIntendedPurposeSummary() { return intendedPurposeSummary; }
    public void setIntendedPurposeSummary(String v) { this.intendedPurposeSummary = v; }
    public String getTechnicalDocumentationReference() { return technicalDocumentationReference; }
    public void setTechnicalDocumentationReference(String v) { this.technicalDocumentationReference = v; }
    public String getEudbId() { return eudbId; }
    public void setEudbId(String v) { this.eudbId = v; }
    public EudbRegistrationStatus getStatus() { return status; }
    public void setStatus(EudbRegistrationStatus v) { this.status = v; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant v) { this.submittedAt = v; }
    public UUID getSubmittedByUserId() { return submittedByUserId; }
    public void setSubmittedByUserId(UUID v) { this.submittedByUserId = v; }
    public Instant getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Instant v) { this.registrationDate = v; }
    public Instant getLastUpdateDate() { return lastUpdateDate; }
    public void setLastUpdateDate(Instant v) { this.lastUpdateDate = v; }
    public String getLastUpdateSummary() { return lastUpdateSummary; }
    public void setLastUpdateSummary(String v) { this.lastUpdateSummary = v; }
    public Instant getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(Instant v) { this.rejectedAt = v; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String v) { this.rejectionReason = v; }
    public Instant getRetiredAt() { return retiredAt; }
    public void setRetiredAt(Instant v) { this.retiredAt = v; }
    public String getRetirementReason() { return retirementReason; }
    public void setRetirementReason(String v) { this.retirementReason = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
