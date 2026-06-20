package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.standards.normdoc.application.NormDocActorProvider;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocEventPublisher;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocService;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocStandardLookup;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerator;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Câblage hexagonal du module de génération de documents normatifs (§8.8). */
@Configuration
public class NormDocBeanConfiguration {

    @Bean
    public NormDocService normDocService(
            NormDocRepository repo,
            NormDocGenerator generator,
            NormDocStandardLookup standards,
            @Qualifier("normDocTenantContextProvider") NormDocTenantProvider tenantProvider,
            @Qualifier("normDocCurrentUserActorProvider") NormDocActorProvider actorProvider,
            NormDocEventPublisher events,
            Clock clock) {
        return new NormDocService(repo, generator, standards,
                tenantProvider, actorProvider, events, clock);
    }
}
