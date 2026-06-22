package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.marketplace.domain.InstallationStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketplace_installations",
        indexes = {
                @Index(name = "idx_mpi_tenant", columnList = "tenant_id"),
                @Index(name = "idx_mpi_tenant_pack", columnList = "tenant_id, marketplace_pack_id")
        })
public class MarketplaceInstallationJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "marketplace_pack_id", nullable = false, updatable = false)
    private UUID marketplacePackId;

    @Column(name = "pack_id", nullable = false, length = 64, updatable = false)
    private String packId;

    @Column(name = "pack_version", nullable = false, length = 32, updatable = false)
    private String packVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InstallationStatus status;

    @Column(name = "installed_by", nullable = false, updatable = false)
    private UUID installedBy;

    @Column(name = "installed_at", nullable = false, updatable = false)
    private Instant installedAt;

    @Column(name = "uninstalled_by")
    private UUID uninstalledBy;

    @Column(name = "uninstalled_at")
    private Instant uninstalledAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getMarketplacePackId() { return marketplacePackId; }
    public void setMarketplacePackId(UUID marketplacePackId) { this.marketplacePackId = marketplacePackId; }
    public String getPackId() { return packId; }
    public void setPackId(String packId) { this.packId = packId; }
    public String getPackVersion() { return packVersion; }
    public void setPackVersion(String packVersion) { this.packVersion = packVersion; }
    public InstallationStatus getStatus() { return status; }
    public void setStatus(InstallationStatus status) { this.status = status; }
    public UUID getInstalledBy() { return installedBy; }
    public void setInstalledBy(UUID installedBy) { this.installedBy = installedBy; }
    public Instant getInstalledAt() { return installedAt; }
    public void setInstalledAt(Instant installedAt) { this.installedAt = installedAt; }
    public UUID getUninstalledBy() { return uninstalledBy; }
    public void setUninstalledBy(UUID uninstalledBy) { this.uninstalledBy = uninstalledBy; }
    public Instant getUninstalledAt() { return uninstalledAt; }
    public void setUninstalledAt(Instant uninstalledAt) { this.uninstalledAt = uninstalledAt; }
}
