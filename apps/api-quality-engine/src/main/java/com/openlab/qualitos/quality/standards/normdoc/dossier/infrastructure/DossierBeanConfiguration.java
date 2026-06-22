package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.openlab.qualitos.quality.standards.normdoc.application.NormDocActorProvider;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocStandardLookup;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerator;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocRepository;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierEventPublisher;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierIntegrityPort;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierReuseLookup;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierService;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierPlanProvider;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Câblage hexagonal du module de génération documentaire multi-documents (§8.8). */
@Configuration
public class DossierBeanConfiguration {

    @Bean
    @SuppressWarnings("checkstyle:ParameterNumber")
    public DossierService dossierService(
            DossierRepository dossiers,
            DossierPlanProvider planProvider,
            NormDocGenerator generator,
            NormDocRepository normDocs,
            NormDocStandardLookup standards,
            DossierReuseLookup reuse,
            DossierIntegrityPort integrity,
            @Qualifier("normDocTenantContextProvider") NormDocTenantProvider tenantProvider,
            @Qualifier("normDocCurrentUserActorProvider") NormDocActorProvider actorProvider,
            DossierEventPublisher events,
            Clock clock) {
        return new DossierService(dossiers, planProvider, generator, normDocs, standards,
                reuse, integrity, tenantProvider, actorProvider, events, clock);
    }
}
