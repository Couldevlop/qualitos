package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.marketplace.application.MarketplacePackService;
import com.openlab.qualitos.quality.marketplace.application.SuperAdminProvider;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class MarketplacePackBeanConfiguration {

    @Bean
    public MarketplacePackService marketplacePackService(
            MarketplacePackRepository repo,
            SuperAdminProvider superAdmin,
            Clock clock) {
        return new MarketplacePackService(repo, superAdmin, clock);
    }
}
