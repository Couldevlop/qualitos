package com.openlab.qualitos.quality.aiconformity.infrastructure;

import com.openlab.qualitos.quality.aiconformity.application.ConformityAssessmentEventPublisher;
import com.openlab.qualitos.quality.aiconformity.application.ConformityAssessmentService;
import com.openlab.qualitos.quality.aiconformity.application.TenantProvider;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ConformityBeanConfiguration {

    @Bean
    public ConformityAssessmentService conformityAssessmentService(
            ConformityAssessmentRepository repo,
            @Qualifier("aicaTenantContextProvider") TenantProvider tenantProvider,
            ConformityAssessmentEventPublisher events,
            Clock clock) {
        return new ConformityAssessmentService(repo, tenantProvider, events, clock);
    }
}
