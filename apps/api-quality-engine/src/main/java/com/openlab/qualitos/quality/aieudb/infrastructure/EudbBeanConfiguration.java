package com.openlab.qualitos.quality.aieudb.infrastructure;

import com.openlab.qualitos.quality.aieudb.application.EudbRegistrationEventPublisher;
import com.openlab.qualitos.quality.aieudb.application.EudbRegistrationService;
import com.openlab.qualitos.quality.aieudb.application.TenantProvider;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class EudbBeanConfiguration {

    @Bean
    public EudbRegistrationService eudbRegistrationService(
            EudbRegistrationRepository repo,
            @Qualifier("eudbTenantContextProvider") TenantProvider tenantProvider,
            EudbRegistrationEventPublisher events,
            Clock clock) {
        return new EudbRegistrationService(repo, tenantProvider, events, clock);
    }
}
