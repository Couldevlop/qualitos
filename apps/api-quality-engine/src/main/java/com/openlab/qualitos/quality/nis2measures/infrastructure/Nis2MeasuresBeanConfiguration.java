package com.openlab.qualitos.quality.nis2measures.infrastructure;

import com.openlab.qualitos.quality.nis2measures.application.Nis2MeasureEventPublisher;
import com.openlab.qualitos.quality.nis2measures.application.Nis2MeasureService;
import com.openlab.qualitos.quality.nis2measures.application.TenantProvider;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasureRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class Nis2MeasuresBeanConfiguration {

    @Bean
    public Nis2MeasureService nis2MeasureService(
            Nis2RiskMeasureRepository repo,
            @Qualifier("nis2mTenantContextProvider") TenantProvider tenantProvider,
            Nis2MeasureEventPublisher events,
            Clock clock) {
        return new Nis2MeasureService(repo, tenantProvider, events, clock);
    }
}
