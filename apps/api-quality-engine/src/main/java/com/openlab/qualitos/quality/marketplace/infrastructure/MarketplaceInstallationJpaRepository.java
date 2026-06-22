package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.marketplace.domain.InstallationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketplaceInstallationJpaRepository
        extends JpaRepository<MarketplaceInstallationJpaEntity, UUID> {

    Optional<MarketplaceInstallationJpaEntity>
        findByTenantIdAndMarketplacePackIdAndStatus(UUID tenantId, UUID marketplacePackId,
                                                    InstallationStatus status);

    Optional<MarketplaceInstallationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<MarketplaceInstallationJpaEntity>
        findByTenantIdAndStatusOrderByInstalledAtDesc(UUID tenantId, InstallationStatus status);

    List<MarketplaceInstallationJpaEntity>
        findByTenantIdOrderByInstalledAtDesc(UUID tenantId);
}
