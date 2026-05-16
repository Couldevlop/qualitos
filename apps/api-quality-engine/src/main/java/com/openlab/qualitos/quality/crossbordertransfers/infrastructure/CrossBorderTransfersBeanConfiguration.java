package com.openlab.qualitos.quality.crossbordertransfers.infrastructure;

import com.openlab.qualitos.quality.crossbordertransfers.application.CrossBorderTransferEventPublisher;
import com.openlab.qualitos.quality.crossbordertransfers.application.CrossBorderTransferService;
import com.openlab.qualitos.quality.crossbordertransfers.application.TenantProvider;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CrossBorderTransfersBeanConfiguration {

    @Bean
    public CrossBorderTransferService crossBorderTransferService(
            CrossBorderTransferRepository repo,
            @Qualifier("cbtTenantContextProvider") TenantProvider tenantProvider,
            CrossBorderTransferEventPublisher events,
            Clock clock) {
        return new CrossBorderTransferService(repo, tenantProvider, events, clock);
    }
}
