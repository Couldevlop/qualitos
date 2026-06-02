package com.openlab.qualitos.quality.tenantmodules.infrastructure;

import com.openlab.qualitos.quality.tenantmodules.domain.ActivationStatus;
import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_module_activations",
        indexes = {
                @Index(name = "idx_tma_tenant", columnList = "tenant_id"),
                @Index(name = "idx_tma_tenant_code", columnList = "tenant_id, module_code"),
                @Index(name = "idx_tma_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_tma_due", columnList = "trial_ends_at, expires_at")
        })
public class ModuleActivationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "module_code", nullable = false, length = 64)
    private String moduleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ActivationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_tier", nullable = false, length = 32)
    private BillingTier billingTier;

    // TEXT cote DB — éviter le mapping @Lob→oid (cf. AuditEvent.payloadJson).
    @Column(name = "configuration_json", columnDefinition = "TEXT")
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.LONGVARCHAR)
    private String configurationJson;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "activated_at", nullable = false, updatable = false)
    private Instant activatedAt;

    @Column(name = "activated_by", nullable = false, updatable = false)
    private UUID activatedBy;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    @Column(name = "last_changed_by", nullable = false)
    private UUID lastChangedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public ActivationStatus getStatus() { return status; }
    public void setStatus(ActivationStatus status) { this.status = status; }
    public BillingTier getBillingTier() { return billingTier; }
    public void setBillingTier(BillingTier billingTier) { this.billingTier = billingTier; }
    public String getConfigurationJson() { return configurationJson; }
    public void setConfigurationJson(String configurationJson) { this.configurationJson = configurationJson; }
    public Instant getTrialEndsAt() { return trialEndsAt; }
    public void setTrialEndsAt(Instant trialEndsAt) { this.trialEndsAt = trialEndsAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
    public UUID getActivatedBy() { return activatedBy; }
    public void setActivatedBy(UUID activatedBy) { this.activatedBy = activatedBy; }
    public Instant getStatusChangedAt() { return statusChangedAt; }
    public void setStatusChangedAt(Instant statusChangedAt) { this.statusChangedAt = statusChangedAt; }
    public UUID getLastChangedBy() { return lastChangedBy; }
    public void setLastChangedBy(UUID lastChangedBy) { this.lastChangedBy = lastChangedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
