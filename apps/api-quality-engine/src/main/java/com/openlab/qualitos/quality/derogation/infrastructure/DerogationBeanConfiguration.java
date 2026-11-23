package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.application.ActorProvider;
import com.openlab.qualitos.quality.derogation.application.DerogationAuditPort;
import com.openlab.qualitos.quality.derogation.application.DerogationLinkBuilder;
import com.openlab.qualitos.quality.derogation.application.DerogationMailPort;
import com.openlab.qualitos.quality.derogation.application.DerogationNotificationPort;
import com.openlab.qualitos.quality.derogation.application.DerogationPdfPort;
import com.openlab.qualitos.quality.derogation.application.DerogationService;
import com.openlab.qualitos.quality.derogation.application.DirectoryPort;
import com.openlab.qualitos.quality.derogation.application.TenantProvider;
import com.openlab.qualitos.quality.derogation.domain.DerogationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câble le service de dérogation.
 *
 * <p>Le service est une classe simple, sans annotation : c'est ici, et nulle part
 * ailleurs, qu'il rencontre le cadre. Les dépendances y sont visibles d'un coup
 * d'œil, et un test peut les remplacer une à une sans démarrer Spring.
 */
@Configuration
public class DerogationBeanConfiguration {

    @Bean
    public DerogationService derogationService(
            DerogationRepository repository,
            DerogationAuditPort audit,
            DerogationNotificationPort notifications,
            DerogationMailPort mail,
            DerogationPdfPort pdf,
            DirectoryPort directory,
            @Qualifier("derogationTenantContextProvider") TenantProvider tenants,
            @Qualifier("derogationActorContextProvider") ActorProvider actors,
            DerogationLinkBuilder links,
            Clock clock) {
        return new DerogationService(repository, audit, notifications, mail, pdf,
                directory, tenants, actors, links, clock);
    }
}
