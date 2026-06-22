package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.marketplace.domain.InstallationStatus;
import com.openlab.qualitos.quality.marketplace.domain.MarketplaceInstallation;
import com.openlab.qualitos.quality.marketplace.domain.MarketplaceInstallationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MarketplaceInstallationRepositoryAdapter implements MarketplaceInstallationRepository {

    private final MarketplaceInstallationJpaRepository jpa;

    public MarketplaceInstallationRepositoryAdapter(MarketplaceInstallationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public MarketplaceInstallation save(MarketplaceInstallation installation) {
        MarketplaceInstallationJpaEntity e = MarketplaceInstallationMapper.toEntity(installation);
        if (e.getId() == null) {
            e.setId(UUID.randomUUID());
        }
        MarketplaceInstallationJpaEntity saved = jpa.save(e);
        if (installation.getId() == null) {
            installation.assignId(saved.getId());
        }
        return MarketplaceInstallationMapper.toDomain(saved);
    }

    @Override
    public Optional<MarketplaceInstallation> findActive(UUID tenantId, UUID marketplacePackId) {
        return jpa.findByTenantIdAndMarketplacePackIdAndStatus(
                        tenantId, marketplacePackId, InstallationStatus.INSTALLED)
                .map(MarketplaceInstallationMapper::toDomain);
    }

    @Override
    public Optional<MarketplaceInstallation> findByIdForTenant(UUID tenantId, UUID installationId) {
        return jpa.findByIdAndTenantId(installationId, tenantId)
                .map(MarketplaceInstallationMapper::toDomain);
    }

    @Override
    public List<MarketplaceInstallation> findActiveByTenant(UUID tenantId) {
        return jpa.findByTenantIdAndStatusOrderByInstalledAtDesc(tenantId, InstallationStatus.INSTALLED)
                .stream().map(MarketplaceInstallationMapper::toDomain).toList();
    }

    @Override
    public List<MarketplaceInstallation> findAllByTenant(UUID tenantId) {
        return jpa.findByTenantIdOrderByInstalledAtDesc(tenantId)
                .stream().map(MarketplaceInstallationMapper::toDomain).toList();
    }
}
