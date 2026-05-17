package com.openlab.qualitos.quality.aiqms.infrastructure;

import com.openlab.qualitos.quality.aiqms.application.AiQmsEventPublisher;
import com.openlab.qualitos.quality.aiqms.application.AiQmsService;
import com.openlab.qualitos.quality.aiqms.application.TenantProvider;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AiQmsBeanConfiguration {

    @Bean
    public AiQmsService aiQmsService(
            AiQmsRepository repo,
            @Qualifier("aqmsTenantContextProvider") TenantProvider tenantProvider,
            AiQmsEventPublisher events,
            Clock clock) {
        return new AiQmsService(repo, tenantProvider, events, clock);
    }
}
