package com.openlab.qualitos.quality.dpia.infrastructure;

import com.openlab.qualitos.quality.dpia.application.DpiaEventPublisher;
import com.openlab.qualitos.quality.dpia.application.DpiaService;
import com.openlab.qualitos.quality.dpia.application.TenantProvider;
import com.openlab.qualitos.quality.dpia.domain.DpiaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DpiaBeanConfiguration {

    @Bean
    public DpiaService dpiaService(
            DpiaRepository repo,
            @Qualifier("dpiaTenantContextProvider") TenantProvider tenantProvider,
            DpiaEventPublisher events,
            Clock clock) {
        return new DpiaService(repo, tenantProvider, events, clock);
    }
}
