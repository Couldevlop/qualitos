package com.openlab.qualitos.quality.tenantmodules.infrastructure;

import com.openlab.qualitos.quality.tenantmodules.application.ActorProvider;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationEventPublisher;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationService;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleLicense;
import com.openlab.qualitos.quality.tenantmodules.application.TenantProvider;
import com.openlab.qualitos.quality.tenantmodules.application.TenantTierProvider;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationRepository;
import com.openlab.qualitos.licensing.application.Licensing;
import org.springframework.beans.factory.annotation.Value;
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
            Clock clock,
            Licensing licensing) {
        return new ModuleActivationService(
                repo, tenantProvider, tierProvider, actorProvider, events, clock,
                licensing.isOnPrem() ? new LicensedModules(licensing) : ModuleLicense.NONE);
    }

    /**
     * Palier appliqué à tous les tenants tant qu'un module de facturation ne le
     * porte pas par tenant. Réglable (défaut FREE) : figé dans le code, il
     * empêchait même le super administrateur d'ouvrir un module de palier
     * supérieur, sans autre recours que de recompiler.
     */
    @Bean
    @ConditionalOnMissingBean(TenantTierProvider.class)
    public TenantTierProvider defaultTenantTierProvider(
            @Value("${qualitos.modules.default-tier:FREE}") String defaultTier, Licensing licensing) {
        // En on-premise, le palier est celui de la licence (ADR 0082).
        return licensing.isOnPrem() ? new LicensedModules(licensing) : new ConfiguredTenantTierProvider(defaultTier);
    }
}
