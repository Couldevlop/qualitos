package com.openlab.qualitos.quality.controlplan.infrastructure;

import com.openlab.qualitos.quality.controlplan.application.ActorProvider;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanAuditPort;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanService;
import com.openlab.qualitos.quality.controlplan.application.TenantProvider;
import com.openlab.qualitos.quality.controlplan.domain.ControlPlanRepository;
import com.openlab.qualitos.quality.controlplan.domain.FmeaItemLookup;
import com.openlab.qualitos.quality.product.domain.ProductLookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ControlPlanBeanConfiguration {

    @Bean
    public ControlPlanService controlPlanService(
            ControlPlanRepository repo,
            ProductLookup products,
            FmeaItemLookup fmeaItems,
            ControlPlanAuditPort audit,
            @Qualifier("controlPlanTenantContextProvider") TenantProvider tenantProvider,
            @Qualifier("controlPlanActorContextProvider") ActorProvider actorProvider,
            Clock clock) {
        return new ControlPlanService(repo, products, fmeaItems, audit,
                tenantProvider, actorProvider, clock);
    }
}
