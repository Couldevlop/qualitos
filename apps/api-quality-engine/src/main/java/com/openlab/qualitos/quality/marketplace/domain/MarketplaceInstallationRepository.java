package com.openlab.qualitos.quality.marketplace.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port — persistance des installations de packs par tenant (multi-tenant).
 * Toutes les méthodes portent {@code tenantId} explicitement (issu du JWT côté
 * service) — aucune fuite cross-tenant possible.
 */
public interface MarketplaceInstallationRepository {

    MarketplaceInstallation save(MarketplaceInstallation installation);

    Optional<MarketplaceInstallation> findActive(UUID tenantId, UUID marketplacePackId);

    Optional<MarketplaceInstallation> findByIdForTenant(UUID tenantId, UUID installationId);

    /** Installations ACTIVES (INSTALLED) du tenant. */
    List<MarketplaceInstallation> findActiveByTenant(UUID tenantId);

    /** Historique complet (toutes installations) du tenant, plus récentes d'abord. */
    List<MarketplaceInstallation> findAllByTenant(UUID tenantId);
}
