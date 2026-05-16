package com.openlab.qualitos.quality.processoragreements.infrastructure;

import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gdpr_processor_agreements",
        indexes = {
                @Index(name = "idx_dpa_tenant", columnList = "tenant_id"),
                @Index(name = "idx_dpa_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_dpa_expiration", columnList = "expiration_date"),
                @Index(name = "uq_dpa_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class ProcessorAgreementJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(name = "processor_name", nullable = false, length = 250)
    private String processorName;

    @Column(name = "processor_legal_entity", length = 250)
    private String processorLegalEntity;

    @Column(name = "processor_contact", length = 250)
    private String processorContact;

    @Column(name = "processor_dpo_contact", length = 250)
    private String processorDpoContact;

    @Column(name = "processor_country", length = 2)
    private String processorCountry;

    @Column(name = "services_description", nullable = false, length = 4000)
    private String servicesDescription;

    @Column(name = "sub_processor_categories", length = 2000)
    private String subProcessorCategoriesCsv;

    @Column(name = "linked_processing_activity_ids", length = 4000)
    private String linkedProcessingActivityIdsCsv;

    @Column(name = "third_country_transfers", length = 500)
    private String thirdCountryTransfersCsv;

    @Column(name = "transfer_safeguards", length = 4000)
    private String transferSafeguards;

    @Column(name = "contract_document_url", length = 1024)
    private String contractDocumentUrl;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "expiration_date")
    private Instant expirationDate;

    @Column(name = "security_measures", length = 4000)
    private String securityMeasures;

    @Column(name = "breach_notification_hours", nullable = false)
    private int breachNotificationCommitmentHours;

    @Column(name = "audit_rights", nullable = false)
    private boolean auditRights;

    @Column(name = "audit_rights_notes", length = 4000)
    private String auditRightsNotes;

    @Column(name = "data_return_or_deletion_terms", length = 4000)
    private String dataReturnOrDeletionTerms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProcessorAgreementStatus status;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(name = "termination_reason", length = 2000)
    private String terminationReason;

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
    public String getProcessorName() { return processorName; }
    public void setProcessorName(String v) { this.processorName = v; }
    public String getProcessorLegalEntity() { return processorLegalEntity; }
    public void setProcessorLegalEntity(String v) { this.processorLegalEntity = v; }
    public String getProcessorContact() { return processorContact; }
    public void setProcessorContact(String v) { this.processorContact = v; }
    public String getProcessorDpoContact() { return processorDpoContact; }
    public void setProcessorDpoContact(String v) { this.processorDpoContact = v; }
    public String getProcessorCountry() { return processorCountry; }
    public void setProcessorCountry(String v) { this.processorCountry = v; }
    public String getServicesDescription() { return servicesDescription; }
    public void setServicesDescription(String v) { this.servicesDescription = v; }
    public String getSubProcessorCategoriesCsv() { return subProcessorCategoriesCsv; }
    public void setSubProcessorCategoriesCsv(String v) { this.subProcessorCategoriesCsv = v; }
    public String getLinkedProcessingActivityIdsCsv() { return linkedProcessingActivityIdsCsv; }
    public void setLinkedProcessingActivityIdsCsv(String v) { this.linkedProcessingActivityIdsCsv = v; }
    public String getThirdCountryTransfersCsv() { return thirdCountryTransfersCsv; }
    public void setThirdCountryTransfersCsv(String v) { this.thirdCountryTransfersCsv = v; }
    public String getTransferSafeguards() { return transferSafeguards; }
    public void setTransferSafeguards(String v) { this.transferSafeguards = v; }
    public String getContractDocumentUrl() { return contractDocumentUrl; }
    public void setContractDocumentUrl(String v) { this.contractDocumentUrl = v; }
    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant v) { this.signedAt = v; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant v) { this.effectiveFrom = v; }
    public Instant getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Instant v) { this.expirationDate = v; }
    public String getSecurityMeasures() { return securityMeasures; }
    public void setSecurityMeasures(String v) { this.securityMeasures = v; }
    public int getBreachNotificationCommitmentHours() { return breachNotificationCommitmentHours; }
    public void setBreachNotificationCommitmentHours(int v) { this.breachNotificationCommitmentHours = v; }
    public boolean isAuditRights() { return auditRights; }
    public void setAuditRights(boolean v) { this.auditRights = v; }
    public String getAuditRightsNotes() { return auditRightsNotes; }
    public void setAuditRightsNotes(String v) { this.auditRightsNotes = v; }
    public String getDataReturnOrDeletionTerms() { return dataReturnOrDeletionTerms; }
    public void setDataReturnOrDeletionTerms(String v) { this.dataReturnOrDeletionTerms = v; }
    public ProcessorAgreementStatus getStatus() { return status; }
    public void setStatus(ProcessorAgreementStatus v) { this.status = v; }
    public Instant getTerminatedAt() { return terminatedAt; }
    public void setTerminatedAt(Instant v) { this.terminatedAt = v; }
    public String getTerminationReason() { return terminationReason; }
    public void setTerminationReason(String v) { this.terminationReason = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
