package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditActorProvider;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditAdoptionLookup;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditEventPublisher;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditService;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditTenantProvider;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditGenerator;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRunRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Câblage hexagonal du module d'audit blanc IA avancé (§8.4 onglet 7). */
@Configuration
public class MockAuditBeanConfiguration {

    @Bean
    public MockAuditService mockAuditService(
            MockAuditAdoptionLookup adoptions,
            MockAuditGenerator generator,
            MockAuditRunRepository repository,
            @Qualifier("mockAuditTenantContextProvider") MockAuditTenantProvider tenantProvider,
            @Qualifier("mockAuditCurrentUserActorProvider") MockAuditActorProvider actorProvider,
            MockAuditEventPublisher events,
            Clock clock) {
        return new MockAuditService(adoptions, generator, repository,
                tenantProvider, actorProvider, events, clock);
    }
}
