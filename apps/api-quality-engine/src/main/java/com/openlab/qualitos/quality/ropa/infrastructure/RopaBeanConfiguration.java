package com.openlab.qualitos.quality.ropa.infrastructure;

import com.openlab.qualitos.quality.ropa.application.ProcessingActivityEventPublisher;
import com.openlab.qualitos.quality.ropa.application.ProcessingActivityService;
import com.openlab.qualitos.quality.ropa.application.TenantProvider;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RopaBeanConfiguration {

    @Bean
    public ProcessingActivityService processingActivityService(
            ProcessingActivityRepository repo,
            @Qualifier("ropaTenantContextProvider") TenantProvider tenantProvider,
            ProcessingActivityEventPublisher events,
            Clock clock) {
        return new ProcessingActivityService(repo, tenantProvider, events, clock);
    }
}
