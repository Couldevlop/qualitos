package com.openlab.qualitos.quality.automateddecisions.infrastructure;

import com.openlab.qualitos.quality.automateddecisions.domain.Art22LawfulBasis;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStatus;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gdpr_automated_decisions",
        indexes = {
                @Index(name = "idx_adm_tenant", columnList = "tenant_id"),
                @Index(name = "idx_adm_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "uq_adm_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class AutomatedDecisionJpaEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 64)
    private AutomatedDecisionType decisionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "art22_lawful_basis", length = 32)
    private Art22LawfulBasis art22LawfulBasis;

    @Column(name = "lawful_basis_details", length = 4000)
    private String lawfulBasisDetails;

    @Column(name = "input_data_categories", length = 2000)
    private String inputDataCategoriesCsv;

    @Column(name = "linked_processing_activity_ids", length = 4000)
    private String linkedProcessingActivityIdsCsv;

    @Column(name = "linked_dpia_id")
    private UUID linkedDpiaId;

    @Column(name = "algorithm_description", length = 8000)
    private String algorithmDescription;

    @Column(name = "significance_for_subject", length = 4000)
    private String significanceForSubject;

    @Column(name = "human_review_mechanism", length = 4000)
    private String humanReviewMechanism;

    @Column(name = "objection_mechanism", length = 4000)
    private String objectionMechanism;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AutomatedDecisionStatus status;

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
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public AutomatedDecisionType getDecisionType() { return decisionType; }
    public void setDecisionType(AutomatedDecisionType v) { this.decisionType = v; }
    public Art22LawfulBasis getArt22LawfulBasis() { return art22LawfulBasis; }
    public void setArt22LawfulBasis(Art22LawfulBasis v) { this.art22LawfulBasis = v; }
    public String getLawfulBasisDetails() { return lawfulBasisDetails; }
    public void setLawfulBasisDetails(String v) { this.lawfulBasisDetails = v; }
    public String getInputDataCategoriesCsv() { return inputDataCategoriesCsv; }
    public void setInputDataCategoriesCsv(String v) { this.inputDataCategoriesCsv = v; }
    public String getLinkedProcessingActivityIdsCsv() { return linkedProcessingActivityIdsCsv; }
    public void setLinkedProcessingActivityIdsCsv(String v) { this.linkedProcessingActivityIdsCsv = v; }
    public UUID getLinkedDpiaId() { return linkedDpiaId; }
    public void setLinkedDpiaId(UUID v) { this.linkedDpiaId = v; }
    public String getAlgorithmDescription() { return algorithmDescription; }
    public void setAlgorithmDescription(String v) { this.algorithmDescription = v; }
    public String getSignificanceForSubject() { return significanceForSubject; }
    public void setSignificanceForSubject(String v) { this.significanceForSubject = v; }
    public String getHumanReviewMechanism() { return humanReviewMechanism; }
    public void setHumanReviewMechanism(String v) { this.humanReviewMechanism = v; }
    public String getObjectionMechanism() { return objectionMechanism; }
    public void setObjectionMechanism(String v) { this.objectionMechanism = v; }
    public AutomatedDecisionStatus getStatus() { return status; }
    public void setStatus(AutomatedDecisionStatus v) { this.status = v; }
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
