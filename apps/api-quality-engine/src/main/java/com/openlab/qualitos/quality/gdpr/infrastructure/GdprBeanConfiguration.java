package com.openlab.qualitos.quality.gdpr.infrastructure;

import com.openlab.qualitos.quality.gdpr.application.SubjectRequestEventPublisher;
import com.openlab.qualitos.quality.gdpr.application.SubjectRequestService;
import com.openlab.qualitos.quality.gdpr.application.TenantProvider;
import com.openlab.qualitos.quality.gdpr.domain.DataSubjectRequestRepository;
import com.openlab.qualitos.quality.gdpr.domain.SubjectIdentifierHasher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class GdprBeanConfiguration {

    @Bean
    public SubjectRequestService subjectRequestService(
            DataSubjectRequestRepository repo,
            SubjectIdentifierHasher hasher,
            @Qualifier("gdprTenantContextProvider") TenantProvider tenantProvider,
            SubjectRequestEventPublisher events,
            Clock clock) {
        return new SubjectRequestService(repo, hasher, tenantProvider, events, clock);
    }
}
