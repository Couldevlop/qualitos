package com.openlab.qualitos.quality.aiact.infrastructure;

import com.openlab.qualitos.quality.aiact.application.AiSystemEventPublisher;
import com.openlab.qualitos.quality.aiact.application.AiSystemService;
import com.openlab.qualitos.quality.aiact.application.TenantProvider;
import com.openlab.qualitos.quality.aiact.domain.AiSystemRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AiActBeanConfiguration {

    @Bean
    public AiSystemService aiSystemService(
            AiSystemRepository repo,
            @Qualifier("aisTenantContextProvider") TenantProvider tenantProvider,
            AiSystemEventPublisher events,
            Clock clock) {
        return new AiSystemService(repo, tenantProvider, events, clock);
    }
}
