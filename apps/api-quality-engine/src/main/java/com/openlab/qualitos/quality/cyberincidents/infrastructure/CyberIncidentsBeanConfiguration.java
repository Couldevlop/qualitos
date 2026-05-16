package com.openlab.qualitos.quality.cyberincidents.infrastructure;

import com.openlab.qualitos.quality.cyberincidents.application.CyberIncidentEventPublisher;
import com.openlab.qualitos.quality.cyberincidents.application.CyberIncidentService;
import com.openlab.qualitos.quality.cyberincidents.application.TenantProvider;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CyberIncidentsBeanConfiguration {

    @Bean
    public CyberIncidentService cyberIncidentService(
            CyberIncidentRepository repo,
            @Qualifier("cybTenantContextProvider") TenantProvider tenantProvider,
            CyberIncidentEventPublisher events,
            Clock clock) {
        return new CyberIncidentService(repo, tenantProvider, events, clock);
    }
}
