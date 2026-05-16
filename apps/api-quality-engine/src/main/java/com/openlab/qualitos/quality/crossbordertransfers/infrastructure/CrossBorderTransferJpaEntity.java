package com.openlab.qualitos.quality.crossbordertransfers.infrastructure;

import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStatus;
import com.openlab.qualitos.quality.crossbordertransfers.domain.TransferMechanism;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gdpr_cross_border_transfers",
        indexes = {
                @Index(name = "idx_cbt_tenant", columnList = "tenant_id"),
                @Index(name = "idx_cbt_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "uq_cbt_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class CrossBorderTransferJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(name = "recipient_name", nullable = false, length = 250)
    private String recipientName;

    @Column(name = "recipient_legal_entity", length = 250)
    private String recipientLegalEntity;

    @Column(name = "recipient_contact", length = 250)
    private String recipientContact;

    @Column(name = "destination_countries", length = 500)
    private String destinationCountriesCsv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private TransferMechanism mechanism;

    @Column(name = "safeguards_description", length = 4000)
    private String safeguardsDescription;

    @Column(name = "safeguards_document_url", length = 1024)
    private String safeguardsDocumentUrl;

    @Column(name = "derogation_justification", length = 4000)
    private String derogationJustification;

    @Column(name = "data_categories", length = 2000)
    private String dataCategoriesCsv;

    @Column(name = "linked_processing_activity_ids", length = 4000)
    private String linkedProcessingActivityIdsCsv;

    @Column(name = "linked_processor_agreement_ids", length = 4000)
    private String linkedProcessorAgreementIdsCsv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CrossBorderTransferStatus status;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspension_reason", length = 2000)
    private String suspensionReason;

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
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String v) { this.recipientName = v; }
    public String getRecipientLegalEntity() { return recipientLegalEntity; }
    public void setRecipientLegalEntity(String v) { this.recipientLegalEntity = v; }
    public String getRecipientContact() { return recipientContact; }
    public void setRecipientContact(String v) { this.recipientContact = v; }
    public String getDestinationCountriesCsv() { return destinationCountriesCsv; }
    public void setDestinationCountriesCsv(String v) { this.destinationCountriesCsv = v; }
    public TransferMechanism getMechanism() { return mechanism; }
    public void setMechanism(TransferMechanism v) { this.mechanism = v; }
    public String getSafeguardsDescription() { return safeguardsDescription; }
    public void setSafeguardsDescription(String v) { this.safeguardsDescription = v; }
    public String getSafeguardsDocumentUrl() { return safeguardsDocumentUrl; }
    public void setSafeguardsDocumentUrl(String v) { this.safeguardsDocumentUrl = v; }
    public String getDerogationJustification() { return derogationJustification; }
    public void setDerogationJustification(String v) { this.derogationJustification = v; }
    public String getDataCategoriesCsv() { return dataCategoriesCsv; }
    public void setDataCategoriesCsv(String v) { this.dataCategoriesCsv = v; }
    public String getLinkedProcessingActivityIdsCsv() { return linkedProcessingActivityIdsCsv; }
    public void setLinkedProcessingActivityIdsCsv(String v) { this.linkedProcessingActivityIdsCsv = v; }
    public String getLinkedProcessorAgreementIdsCsv() { return linkedProcessorAgreementIdsCsv; }
    public void setLinkedProcessorAgreementIdsCsv(String v) { this.linkedProcessorAgreementIdsCsv = v; }
    public CrossBorderTransferStatus getStatus() { return status; }
    public void setStatus(CrossBorderTransferStatus v) { this.status = v; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant v) { this.effectiveFrom = v; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant v) { this.effectiveTo = v; }
    public Instant getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(Instant v) { this.suspendedAt = v; }
    public String getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(String v) { this.suspensionReason = v; }
    public String getTerminationReason() { return terminationReason; }
    public void setTerminationReason(String v) { this.terminationReason = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
