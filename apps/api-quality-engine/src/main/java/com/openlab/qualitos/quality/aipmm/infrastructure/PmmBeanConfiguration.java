package com.openlab.qualitos.quality.aipmm.infrastructure;

import com.openlab.qualitos.quality.aipmm.application.PmmPlanEventPublisher;
import com.openlab.qualitos.quality.aipmm.application.PmmPlanService;
import com.openlab.qualitos.quality.aipmm.application.TenantProvider;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PmmBeanConfiguration {

    @Bean
    public PmmPlanService pmmPlanService(
            PmmPlanRepository repo,
            @Qualifier("pmmTenantContextProvider") TenantProvider tenantProvider,
            PmmPlanEventPublisher events,
            Clock clock) {
        return new PmmPlanService(repo, tenantProvider, events, clock);
    }
}
