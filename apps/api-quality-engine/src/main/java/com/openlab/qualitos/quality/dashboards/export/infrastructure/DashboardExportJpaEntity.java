package com.openlab.qualitos.quality.dashboards.export.infrastructure;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the signed dashboard export receipt (table {@code dashboard_exports},
 * migration V96). The signature envelope is a long Base64URL blob → TEXT column.
 */
@Entity
@Table(name = "dashboard_exports")
public class DashboardExportJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "dashboard_id", nullable = false, updatable = false)
    private UUID dashboardId;

    @Column(name = "dashboard_name", nullable = false, length = 200)
    private String dashboardName;

    @Column(name = "verification_code", nullable = false, unique = true, length = 64)
    private String verificationCode;

    @Column(name = "sha256_hex", nullable = false, length = 64)
    private String sha256Hex;

    @Column(name = "signature_envelope", nullable = false, columnDefinition = "TEXT")
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String signatureEnvelope;

    @Column(name = "anchor_tx_ref", nullable = false, length = 200)
    private String anchorTxRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getDashboardId() { return dashboardId; }
    public void setDashboardId(UUID dashboardId) { this.dashboardId = dashboardId; }
    public String getDashboardName() { return dashboardName; }
    public void setDashboardName(String dashboardName) { this.dashboardName = dashboardName; }
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    public String getSha256Hex() { return sha256Hex; }
    public void setSha256Hex(String sha256Hex) { this.sha256Hex = sha256Hex; }
    public String getSignatureEnvelope() { return signatureEnvelope; }
    public void setSignatureEnvelope(String signatureEnvelope) { this.signatureEnvelope = signatureEnvelope; }
    public String getAnchorTxRef() { return anchorTxRef; }
    public void setAnchorTxRef(String anchorTxRef) { this.anchorTxRef = anchorTxRef; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
