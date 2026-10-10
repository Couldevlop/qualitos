package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.audit.AuditFindingRepository;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.change.ChangeRequestRepository;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import com.openlab.qualitos.quality.risk.FmeaItemRepository;
import com.openlab.qualitos.quality.risk.FmeaProjectRepository;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage Spring du registre des risques et opportunités.
 *
 * <p>Le {@code Clock} vient de {@code CommonBeansConfiguration}, unique et
 * {@code @Primary} : en déclarer un second ici rendrait l'injection ambiguë.
 */
@Configuration
public class RiskRegisterBeanConfiguration {

    @Bean
    @SuppressWarnings("java:S107") // un dépôt par port
    public RiskRegisterService riskRegisterService(RiskJpaRepository risks,
                                                   OpportunityJpaRepository opportunities,
                                                   OpportunityActionJpaRepository actions,
                                                   RegisterEventJpaRepository events,
                                                   CapaService capaService,
                                                   CapaCaseRepository capaCases,
                                                   FmeaItemRepository fmeaItems,
                                                   FmeaProjectRepository fmeaProjects,
                                                   NonConformityRepository nonConformities,
                                                   AuditFindingRepository auditFindings,
                                                   ChangeRequestRepository changes,
                                                   AuditEventService auditEvents,
                                                   RecordScope scope,
                                                   Clock clock) {
        return new RiskRegisterService(
                new RegisterRepositoryAdapters.Risks(risks),
                new RegisterRepositoryAdapters.Opportunities(opportunities),
                new RegisterRepositoryAdapters.Actions(actions),
                new RegisterRepositoryAdapters.Events(events),
                new CapaRiskGateway(capaService, capaCases),
                new PlatformRiskSourceCatalog(fmeaItems, fmeaProjects, nonConformities, auditFindings, changes, scope),
                new JwtRegisterContext(scope),
                new AuditLogRegisterPublisher(auditEvents),
                clock);
    }
}
