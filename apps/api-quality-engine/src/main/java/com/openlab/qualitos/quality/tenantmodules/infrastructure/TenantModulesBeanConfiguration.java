package com.openlab.qualitos.quality.tenantmodules.infrastructure;

import com.openlab.qualitos.quality.tenantmodules.application.ActorProvider;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationEventPublisher;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationService;
import com.openlab.qualitos.quality.tenantmodules.application.TenantProvider;
import com.openlab.qualitos.quality.tenantmodules.application.TenantTierProvider;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TenantModulesBeanConfiguration {

    @Bean
    public ModuleActivationService moduleActivationService(
            ModuleActivationRepository repo,
            TenantProvider tenantProvider,
            TenantTierProvider tierProvider,
            ActorProvider actorProvider,
            ModuleActivationEventPublisher events,
            Clock clock) {
        return new ModuleActivationService(
                repo, tenantProvider, tierProvider, actorProvider, events, clock);
    }

    /** Tier par défaut FREE tant qu'un module billing concret n'est pas livré. */
    @Bean
    @ConditionalOnMissingBean(TenantTierProvider.class)
    public TenantTierProvider defaultTenantTierProvider() {
        return new TenantTierProvider.FreeByDefault();
    }
}
