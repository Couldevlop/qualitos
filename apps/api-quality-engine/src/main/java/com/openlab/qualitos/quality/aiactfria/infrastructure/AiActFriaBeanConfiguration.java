package com.openlab.qualitos.quality.aiactfria.infrastructure;

import com.openlab.qualitos.quality.aiactfria.application.FriaEventPublisher;
import com.openlab.qualitos.quality.aiactfria.application.FriaService;
import com.openlab.qualitos.quality.aiactfria.application.TenantProvider;
import com.openlab.qualitos.quality.aiactfria.domain.FriaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AiActFriaBeanConfiguration {

    @Bean
    public FriaService friaService(
            FriaRepository repo,
            @Qualifier("friaTenantContextProvider") TenantProvider tenantProvider,
            FriaEventPublisher events,
            Clock clock) {
        return new FriaService(repo, tenantProvider, events, clock);
    }
}
