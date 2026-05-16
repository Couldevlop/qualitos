package com.openlab.qualitos.quality.processoragreements.infrastructure;

import com.openlab.qualitos.quality.processoragreements.application.ProcessorAgreementEventPublisher;
import com.openlab.qualitos.quality.processoragreements.application.ProcessorAgreementService;
import com.openlab.qualitos.quality.processoragreements.application.TenantProvider;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ProcessorAgreementsBeanConfiguration {

    @Bean
    public ProcessorAgreementService processorAgreementService(
            ProcessorAgreementRepository repo,
            @Qualifier("dpaTenantContextProvider") TenantProvider tenantProvider,
            ProcessorAgreementEventPublisher events,
            Clock clock) {
        return new ProcessorAgreementService(repo, tenantProvider, events, clock);
    }
}
