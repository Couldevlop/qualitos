package com.openlab.qualitos.quality.ratelimit.infrastructure;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rate_limit_policies",
        uniqueConstraints = @UniqueConstraint(name = "uk_rate_limit_tenant_scope",
                columnNames = {"tenant_id", "scope"}),
        indexes = {
                @Index(name = "idx_rate_limit_tenant", columnList = "tenant_id"),
                @Index(name = "idx_rate_limit_tenant_enabled", columnList = "tenant_id, enabled")
        })
public class RateLimitPolicyJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String scope;

    @Column(name = "window_seconds", nullable = false)
    private int windowSeconds;

    @Column(name = "max_requests", nullable = false)
    private int maxRequests;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public int getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(int v) { this.windowSeconds = v; }
    public int getMaxRequests() { return maxRequests; }
    public void setMaxRequests(int v) { this.maxRequests = v; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
