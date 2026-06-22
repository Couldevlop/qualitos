package com.openlab.qualitos.quality.marketplace.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate — installation d'un pack marketplace PUBLIÉ par un tenant (CLAUDE.md §8.11).
 *
 * <p>Multi-tenant STRICT : {@code tenantId} provient toujours du JWT (jamais du body).
 * Une installation matérialise l'adoption du pack par le tenant ; elle s'articule
 * avec le mécanisme d'Industry Packs en portant le {@code packId} (code) du pack,
 * qui sert de clé d'activation côté Industry Pack lorsqu'un pack homonyme existe.</p>
 *
 * <p>Un couple (tenant, marketplacePackId) ne peut avoir qu'UNE installation ACTIVE
 * (INSTALLED) — la désinstallation flippe le statut, l'enregistrement est conservé.</p>
 */
public final class MarketplaceInstallation {

    private UUID id;
    private final UUID tenantId;
    private final UUID marketplacePackId;
    /** Code business du pack (packId), copié pour traçabilité et bridge Industry Pack. */
    private final String packId;
    private final String packVersion;
    private InstallationStatus status;
    private final UUID installedBy;
    private final Instant installedAt;
    private UUID uninstalledBy;
    private Instant uninstalledAt;

    public MarketplaceInstallation(UUID id, UUID tenantId, UUID marketplacePackId,
                                   String packId, String packVersion,
                                   InstallationStatus status,
                                   UUID installedBy, Instant installedAt,
                                   UUID uninstalledBy, Instant uninstalledAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.marketplacePackId = Objects.requireNonNull(marketplacePackId, "marketplacePackId");
        this.packId = Objects.requireNonNull(packId, "packId");
        this.packVersion = Objects.requireNonNull(packVersion, "packVersion");
        this.status = Objects.requireNonNull(status, "status");
        this.installedBy = Objects.requireNonNull(installedBy, "installedBy");
        this.installedAt = Objects.requireNonNull(installedAt, "installedAt");
        this.uninstalledBy = uninstalledBy;
        this.uninstalledAt = uninstalledAt;
    }

    public static MarketplaceInstallation install(UUID tenantId, MarketplacePack pack,
                                                  UUID installedBy, Instant now) {
        if (pack.getStatus() != MarketplacePackStatus.PUBLISHED) {
            throw new MarketplacePackStateException(
                    "only a PUBLISHED pack can be installed (status=" + pack.getStatus() + ")");
        }
        return new MarketplaceInstallation(null, tenantId, pack.getId(),
                pack.getPackId(), pack.getVersion(),
                InstallationStatus.INSTALLED, installedBy, now, null, null);
    }

    public void uninstall(UUID actor, Instant now) {
        Objects.requireNonNull(actor, "actor");
        if (status != InstallationStatus.INSTALLED) {
            throw new MarketplacePackStateException("installation is not active");
        }
        this.status = InstallationStatus.UNINSTALLED;
        this.uninstalledBy = actor;
        this.uninstalledAt = now;
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getMarketplacePackId() { return marketplacePackId; }
    public String getPackId() { return packId; }
    public String getPackVersion() { return packVersion; }
    public InstallationStatus getStatus() { return status; }
    public UUID getInstalledBy() { return installedBy; }
    public Instant getInstalledAt() { return installedAt; }
    public UUID getUninstalledBy() { return uninstalledBy; }
    public Instant getUninstalledAt() { return uninstalledAt; }
}
