package com.openlab.qualitos.quality.ropa.infrastructure;

import com.openlab.qualitos.quality.ropa.domain.LawfulBasis;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gdpr_processing_activities",
        indexes = {
                @Index(name = "idx_ropa_tenant", columnList = "tenant_id"),
                @Index(name = "idx_ropa_tenant_status",
                        columnList = "tenant_id, status"),
                @Index(name = "uq_ropa_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class ProcessingActivityJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(nullable = false, length = 4000)
    private String purposes;

    @Enumerated(EnumType.STRING)
    @Column(name = "lawful_basis", nullable = false, length = 32)
    private LawfulBasis lawfulBasis;

    @Column(name = "lawful_basis_details", length = 4000)
    private String lawfulBasisDetails;

    @Column(name = "controller_name", nullable = false, length = 250)
    private String controllerName;

    @Column(name = "controller_contact", nullable = false, length = 250)
    private String controllerContact;

    @Column(name = "dpo_contact", length = 250)
    private String dpoContact;

    @Column(name = "joint_controller_name", length = 250)
    private String jointControllerName;

    @Column(name = "joint_controller_contact", length = 250)
    private String jointControllerContact;

    /** CSV de codes (regex validé par l'agrégat). */
    @Column(name = "data_subject_categories", length = 2000)
    private String dataSubjectCategoriesCsv;

    @Column(name = "data_categories", length = 2000)
    private String dataCategoriesCsv;

    @Column(name = "special_categories_processed", nullable = false)
    private boolean specialCategoriesProcessed;

    @Column(name = "special_categories_justification", length = 4000)
    private String specialCategoriesJustification;

    @Column(name = "recipient_categories", length = 2000)
    private String recipientCategoriesCsv;

    /** CSV ISO 3166-1 alpha-2 (FR,US,IN,...). */
    @Column(name = "third_country_transfers", length = 500)
    private String thirdCountryTransfersCsv;

    @Column(name = "transfer_safeguards", length = 4000)
    private String transferSafeguards;

    /** CSV d'UUID. */
    @Column(name = "linked_retention_rule_ids", length = 4000)
    private String linkedRetentionRuleIdsCsv;

    @Column(name = "technical_measures", length = 4000)
    private String technicalMeasures;

    @Column(name = "organizational_measures", length = 4000)
    private String organizationalMeasures;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProcessingActivityStatus status;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

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
    public String getPurposes() { return purposes; }
    public void setPurposes(String v) { this.purposes = v; }
    public LawfulBasis getLawfulBasis() { return lawfulBasis; }
    public void setLawfulBasis(LawfulBasis v) { this.lawfulBasis = v; }
    public String getLawfulBasisDetails() { return lawfulBasisDetails; }
    public void setLawfulBasisDetails(String v) { this.lawfulBasisDetails = v; }
    public String getControllerName() { return controllerName; }
    public void setControllerName(String v) { this.controllerName = v; }
    public String getControllerContact() { return controllerContact; }
    public void setControllerContact(String v) { this.controllerContact = v; }
    public String getDpoContact() { return dpoContact; }
    public void setDpoContact(String v) { this.dpoContact = v; }
    public String getJointControllerName() { return jointControllerName; }
    public void setJointControllerName(String v) { this.jointControllerName = v; }
    public String getJointControllerContact() { return jointControllerContact; }
    public void setJointControllerContact(String v) { this.jointControllerContact = v; }
    public String getDataSubjectCategoriesCsv() { return dataSubjectCategoriesCsv; }
    public void setDataSubjectCategoriesCsv(String v) { this.dataSubjectCategoriesCsv = v; }
    public String getDataCategoriesCsv() { return dataCategoriesCsv; }
    public void setDataCategoriesCsv(String v) { this.dataCategoriesCsv = v; }
    public boolean isSpecialCategoriesProcessed() { return specialCategoriesProcessed; }
    public void setSpecialCategoriesProcessed(boolean v) { this.specialCategoriesProcessed = v; }
    public String getSpecialCategoriesJustification() { return specialCategoriesJustification; }
    public void setSpecialCategoriesJustification(String v) { this.specialCategoriesJustification = v; }
    public String getRecipientCategoriesCsv() { return recipientCategoriesCsv; }
    public void setRecipientCategoriesCsv(String v) { this.recipientCategoriesCsv = v; }
    public String getThirdCountryTransfersCsv() { return thirdCountryTransfersCsv; }
    public void setThirdCountryTransfersCsv(String v) { this.thirdCountryTransfersCsv = v; }
    public String getTransferSafeguards() { return transferSafeguards; }
    public void setTransferSafeguards(String v) { this.transferSafeguards = v; }
    public String getLinkedRetentionRuleIdsCsv() { return linkedRetentionRuleIdsCsv; }
    public void setLinkedRetentionRuleIdsCsv(String v) { this.linkedRetentionRuleIdsCsv = v; }
    public String getTechnicalMeasures() { return technicalMeasures; }
    public void setTechnicalMeasures(String v) { this.technicalMeasures = v; }
    public String getOrganizationalMeasures() { return organizationalMeasures; }
    public void setOrganizationalMeasures(String v) { this.organizationalMeasures = v; }
    public ProcessingActivityStatus getStatus() { return status; }
    public void setStatus(ProcessingActivityStatus v) { this.status = v; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant v) { this.effectiveFrom = v; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant v) { this.effectiveTo = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
