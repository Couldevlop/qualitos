package com.openlab.qualitos.quality.aiconformity.infrastructure;

import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStatus;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityProcedure;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_act_conformity_assessments",
        indexes = {
                @Index(name = "idx_aica_tenant", columnList = "tenant_id"),
                @Index(name = "idx_aica_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_aica_tenant_system", columnList = "tenant_id, ai_system_id"),
                @Index(name = "uq_aica_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class ConformityAssessmentJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(name = "ai_system_id", nullable = false)
    private UUID aiSystemId;

    @Column(name = "qms_id")
    private UUID qmsId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ConformityProcedure procedure;

    @Column(name = "notified_body_id", length = 8)
    private String notifiedBodyId;

    @Column(name = "notified_body_name", length = 250)
    private String notifiedBodyName;

    @Column(nullable = false, length = 4000)
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ConformityAssessmentStatus status;

    @Column(name = "planned_at")
    private Instant plannedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "certified_at")
    private Instant certifiedAt;

    @Column(name = "certificate_number", length = 250)
    private String certificateNumber;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "eu_declaration_reference", length = 250)
    private String euDeclarationReference;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason", length = 2000)
    private String revocationReason;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

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
    public UUID getQmsId() { return qmsId; }
    public void setQmsId(UUID v) { this.qmsId = v; }
    public ConformityProcedure getProcedure() { return procedure; }
    public void setProcedure(ConformityProcedure v) { this.procedure = v; }
    public String getNotifiedBodyId() { return notifiedBodyId; }
    public void setNotifiedBodyId(String v) { this.notifiedBodyId = v; }
    public String getNotifiedBodyName() { return notifiedBodyName; }
    public void setNotifiedBodyName(String v) { this.notifiedBodyName = v; }
    public String getScope() { return scope; }
    public void setScope(String v) { this.scope = v; }
    public ConformityAssessmentStatus getStatus() { return status; }
    public void setStatus(ConformityAssessmentStatus v) { this.status = v; }
    public Instant getPlannedAt() { return plannedAt; }
    public void setPlannedAt(Instant v) { this.plannedAt = v; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant v) { this.startedAt = v; }
    public Instant getCertifiedAt() { return certifiedAt; }
    public void setCertifiedAt(Instant v) { this.certifiedAt = v; }
    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String v) { this.certificateNumber = v; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant v) { this.validUntil = v; }
    public String getEuDeclarationReference() { return euDeclarationReference; }
    public void setEuDeclarationReference(String v) { this.euDeclarationReference = v; }
    public Instant getExpiredAt() { return expiredAt; }
    public void setExpiredAt(Instant v) { this.expiredAt = v; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant v) { this.revokedAt = v; }
    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String v) { this.revocationReason = v; }
    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant v) { this.failedAt = v; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String v) { this.failureReason = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
