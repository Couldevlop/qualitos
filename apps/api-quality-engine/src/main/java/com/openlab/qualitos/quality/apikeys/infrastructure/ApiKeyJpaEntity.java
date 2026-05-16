package com.openlab.qualitos.quality.apikeys.infrastructure;

import com.openlab.qualitos.quality.apikeys.domain.ApiKeyStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys",
        uniqueConstraints = @UniqueConstraint(name = "uk_api_key_prefix", columnNames = "prefix"),
        indexes = {
                @Index(name = "idx_api_key_tenant", columnList = "tenant_id"),
                @Index(name = "idx_api_key_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_api_key_expirable", columnList = "status, expires_at")
        })
public class ApiKeyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 16)
    private String prefix;

    /** Bcrypt — jamais le secret en clair. */
    @Column(name = "hashed_secret", nullable = false, length = 200)
    private String hashedSecret;

    /** CSV trié, ex "audit.read,kpi.write". */
    @Column(name = "scopes_csv", length = 2000)
    private String scopesCsv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApiKeyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getHashedSecret() { return hashedSecret; }
    public void setHashedSecret(String hashedSecret) { this.hashedSecret = hashedSecret; }
    public String getScopesCsv() { return scopesCsv; }
    public void setScopesCsv(String scopesCsv) { this.scopesCsv = scopesCsv; }
    public ApiKeyStatus getStatus() { return status; }
    public void setStatus(ApiKeyStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public UUID getRevokedBy() { return revokedBy; }
    public void setRevokedBy(UUID revokedBy) { this.revokedBy = revokedBy; }
}
