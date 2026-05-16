package com.openlab.qualitos.quality.breach.infrastructure;

import com.openlab.qualitos.quality.breach.application.BreachEventPublisher;
import com.openlab.qualitos.quality.breach.application.BreachService;
import com.openlab.qualitos.quality.breach.application.TenantProvider;
import com.openlab.qualitos.quality.breach.domain.BreachRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BreachBeanConfiguration {

    @Bean
    public BreachService breachService(
            BreachRepository repo,
            @Qualifier("breachTenantContextProvider") TenantProvider tenantProvider,
            BreachEventPublisher events,
            Clock clock) {
        return new BreachService(repo, tenantProvider, events, clock);
    }
}
