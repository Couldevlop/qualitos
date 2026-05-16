package com.openlab.qualitos.quality.consent.infrastructure;

import com.openlab.qualitos.quality.consent.application.ConsentEventPublisher;
import com.openlab.qualitos.quality.consent.application.ConsentService;
import com.openlab.qualitos.quality.consent.application.TenantProvider;
import com.openlab.qualitos.quality.consent.domain.ConsentRepository;
import com.openlab.qualitos.quality.consent.domain.SubjectIdentifierHasher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ConsentBeanConfiguration {

    @Bean
    public ConsentService consentService(
            ConsentRepository repo,
            @Qualifier("consentSha256Hasher") SubjectIdentifierHasher hasher,
            @Qualifier("consentTenantContextProvider") TenantProvider tenantProvider,
            ConsentEventPublisher events,
            Clock clock) {
        return new ConsentService(repo, hasher, tenantProvider, events, clock);
    }
}
