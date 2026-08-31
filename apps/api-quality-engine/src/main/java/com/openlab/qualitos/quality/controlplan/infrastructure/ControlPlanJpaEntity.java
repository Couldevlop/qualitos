package com.openlab.qualitos.quality.controlplan.infrastructure;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "control_plans",
        indexes = @Index(name = "idx_control_plans_product", columnList = "tenant_id, product_id"))
public class ControlPlanJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 20)
    private String phase;

    @Column(nullable = false)
    private int revision;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Empreinte du document scellé, sa signature hybride et la référence de la
     * transaction qui l'ancre. Nuls tant que le plan n'est pas approuvé : un
     * brouillon n'a rien d'opposable à prouver.
     */
    @Column(name = "seal_sha256", length = 64)
    private String sealSha256;

    @Column(name = "seal_signature", length = 20000)
    private String sealSignature;

    @Column(name = "anchor_tx_ref", length = 200)
    private String anchorTxRef;

    /** Version du calcul d'empreinte au scellement ; 0 tant qu'il n'a pas eu lieu. */
    @Column(name = "seal_version", nullable = false)
    private int sealVersion;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID v) { this.productId = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getPhase() { return phase; }
    public void setPhase(String v) { this.phase = v; }
    public int getRevision() { return revision; }
    public void setRevision(int v) { this.revision = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID v) { this.ownerUserId = v; }
    public UUID getApprovedBy() { return approvedBy; }
    public void setApprovedBy(UUID v) { this.approvedBy = v; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant v) { this.approvedAt = v; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID v) { this.createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
    public int getSealVersion() { return sealVersion; }
    public void setSealVersion(int v) { this.sealVersion = v; }
    public String getSealSha256() { return sealSha256; }
    public void setSealSha256(String v) { this.sealSha256 = v; }
    public String getSealSignature() { return sealSignature; }
    public void setSealSignature(String v) { this.sealSignature = v; }
    public String getAnchorTxRef() { return anchorTxRef; }
    public void setAnchorTxRef(String v) { this.anchorTxRef = v; }
}
