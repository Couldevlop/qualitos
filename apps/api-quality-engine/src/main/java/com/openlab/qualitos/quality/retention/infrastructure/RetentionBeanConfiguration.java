package com.openlab.qualitos.quality.retention.infrastructure;

import com.openlab.qualitos.quality.retention.application.RetentionRuleEventPublisher;
import com.openlab.qualitos.quality.retention.application.RetentionRuleService;
import com.openlab.qualitos.quality.retention.application.TenantProvider;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RetentionBeanConfiguration {

    @Bean
    public RetentionRuleService retentionRuleService(
            RetentionRuleRepository repo,
            @Qualifier("retentionTenantContextProvider") TenantProvider tenantProvider,
            RetentionRuleEventPublisher events,
            Clock clock) {
        return new RetentionRuleService(repo, tenantProvider, events, clock);
    }
}
