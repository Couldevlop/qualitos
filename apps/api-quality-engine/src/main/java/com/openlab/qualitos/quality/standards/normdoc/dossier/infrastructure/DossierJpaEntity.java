package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistance d'un dossier documentaire multi-documents (§8.8). La liste des
 * pièces est stockée en JSON via {@code @JdbcTypeCode(LONGVARCHAR)} sur une
 * colonne TEXT — jamais {@code @Lob String} (cf. consigne migration).
 */
@Entity
@Table(name = "standard_doc_dossiers",
        indexes = {
                @Index(name = "idx_sdd_tenant", columnList = "tenant_id"),
                @Index(name = "idx_sdd_tenant_status", columnList = "tenant_id, status")
        })
public class DossierJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "standard_id", nullable = false)
    private UUID standardId;

    @Column(name = "standard_code", nullable = false, length = 100)
    private String standardCode;

    @Column(name = "standard_name", nullable = false, length = 500)
    private String standardName;

    @Column(name = "organization_name", nullable = false, length = 500)
    private String organizationName;

    @Column(nullable = false, length = 16)
    private String language;

    /** Pièces du dossier sérialisées en JSON (TEXT). */
    @Column(name = "documents_json", columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String documentsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DossierStatus status;

    @Column(name = "ai_provider", length = 100)
    private String aiProvider;

    @Column(name = "integrity_sha256", length = 64)
    private String integritySha256;

    @Column(name = "integrity_signature", columnDefinition = "TEXT")
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String integritySignature;

    @Column(name = "anchor_tx_ref", length = 300)
    private String anchorTxRef;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "finalized_by")
    private UUID finalizedByUserId;

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
    public UUID getStandardId() { return standardId; }
    public void setStandardId(UUID v) { this.standardId = v; }
    public String getStandardCode() { return standardCode; }
    public void setStandardCode(String v) { this.standardCode = v; }
    public String getStandardName() { return standardName; }
    public void setStandardName(String v) { this.standardName = v; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String v) { this.organizationName = v; }
    public String getLanguage() { return language; }
    public void setLanguage(String v) { this.language = v; }
    public String getDocumentsJson() { return documentsJson; }
    public void setDocumentsJson(String v) { this.documentsJson = v; }
    public DossierStatus getStatus() { return status; }
    public void setStatus(DossierStatus v) { this.status = v; }
    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String v) { this.aiProvider = v; }
    public String getIntegritySha256() { return integritySha256; }
    public void setIntegritySha256(String v) { this.integritySha256 = v; }
    public String getIntegritySignature() { return integritySignature; }
    public void setIntegritySignature(String v) { this.integritySignature = v; }
    public String getAnchorTxRef() { return anchorTxRef; }
    public void setAnchorTxRef(String v) { this.anchorTxRef = v; }
    public Instant getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Instant v) { this.finalizedAt = v; }
    public UUID getFinalizedByUserId() { return finalizedByUserId; }
    public void setFinalizedByUserId(UUID v) { this.finalizedByUserId = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
