package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.marketplace.domain.MarketplaceInstallation;

final class MarketplaceInstallationMapper {
    private MarketplaceInstallationMapper() {}

    static MarketplaceInstallation toDomain(MarketplaceInstallationJpaEntity e) {
        return new MarketplaceInstallation(
                e.getId(), e.getTenantId(), e.getMarketplacePackId(),
                e.getPackId(), e.getPackVersion(), e.getStatus(),
                e.getInstalledBy(), e.getInstalledAt(),
                e.getUninstalledBy(), e.getUninstalledAt());
    }

    static MarketplaceInstallationJpaEntity toEntity(MarketplaceInstallation i) {
        MarketplaceInstallationJpaEntity e = new MarketplaceInstallationJpaEntity();
        e.setId(i.getId());
        e.setTenantId(i.getTenantId());
        e.setMarketplacePackId(i.getMarketplacePackId());
        e.setPackId(i.getPackId());
        e.setPackVersion(i.getPackVersion());
        e.setStatus(i.getStatus());
        e.setInstalledBy(i.getInstalledBy());
        e.setInstalledAt(i.getInstalledAt());
        e.setUninstalledBy(i.getUninstalledBy());
        e.setUninstalledAt(i.getUninstalledAt());
        return e;
    }
}
