package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.marketplace.application.CurrentActorProvider;
import com.openlab.qualitos.quality.marketplace.application.ManifestScanner;
import com.openlab.qualitos.quality.marketplace.application.MarketplacePackService;
import com.openlab.qualitos.quality.marketplace.application.SuperAdminProvider;
import com.openlab.qualitos.quality.marketplace.application.TenantProvider;
import com.openlab.qualitos.quality.marketplace.domain.MarketplaceInstallationRepository;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage des use-cases marketplace (application Spring-free → wiring en infra).
 */
@Configuration
public class MarketplacePackBeanConfiguration {

    @Bean
    public ManifestScanner marketplaceManifestScanner(ObjectMapper objectMapper) {
        return new ManifestScanner(objectMapper);
    }

    @Bean
    public MarketplacePackService marketplacePackService(
            MarketplacePackRepository packRepo,
            MarketplaceInstallationRepository installRepo,
            SuperAdminProvider superAdmin,
            CurrentActorProvider actor,
            TenantProvider tenant,
            ManifestScanner manifestScanner,
            Clock clock) {
        return new MarketplacePackService(packRepo, installRepo, superAdmin,
                actor, tenant, manifestScanner, clock);
    }
}
