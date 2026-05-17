package com.openlab.qualitos.quality.aiincidents.infrastructure;

import com.openlab.qualitos.quality.aiincidents.application.AiIncidentEventPublisher;
import com.openlab.qualitos.quality.aiincidents.application.AiIncidentService;
import com.openlab.qualitos.quality.aiincidents.application.TenantProvider;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AiIncidentBeanConfiguration {

    @Bean
    public AiIncidentService aiIncidentService(
            AiIncidentRepository repo,
            @Qualifier("aiiTenantContextProvider") TenantProvider tenantProvider,
            AiIncidentEventPublisher events,
            Clock clock) {
        return new AiIncidentService(repo, tenantProvider, events, clock);
    }
}
