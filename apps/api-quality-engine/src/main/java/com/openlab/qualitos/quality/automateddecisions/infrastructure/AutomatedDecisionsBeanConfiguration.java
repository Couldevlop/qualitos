package com.openlab.qualitos.quality.automateddecisions.infrastructure;

import com.openlab.qualitos.quality.automateddecisions.application.AutomatedDecisionEventPublisher;
import com.openlab.qualitos.quality.automateddecisions.application.AutomatedDecisionService;
import com.openlab.qualitos.quality.automateddecisions.application.TenantProvider;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AutomatedDecisionsBeanConfiguration {

    @Bean
    public AutomatedDecisionService automatedDecisionService(
            AutomatedDecisionRepository repo,
            @Qualifier("admTenantContextProvider") TenantProvider tenantProvider,
            AutomatedDecisionEventPublisher events,
            Clock clock) {
        return new AutomatedDecisionService(repo, tenantProvider, events, clock);
    }
}
