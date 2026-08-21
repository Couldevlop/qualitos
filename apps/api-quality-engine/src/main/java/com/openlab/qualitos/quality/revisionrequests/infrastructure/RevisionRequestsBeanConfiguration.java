package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.revisionrequests.application.ActorProvider;
import com.openlab.qualitos.quality.revisionrequests.application.CapaActionsPort;
import com.openlab.qualitos.quality.revisionrequests.application.CapaRevisionTrigger;
import com.openlab.qualitos.quality.revisionrequests.application.ControlPlanDraftPort;
import com.openlab.qualitos.quality.revisionrequests.application.NcHistoryPort;
import com.openlab.qualitos.quality.revisionrequests.application.NcLookupPort;
import com.openlab.qualitos.quality.revisionrequests.application.NcRevisionTrigger;
import com.openlab.qualitos.quality.revisionrequests.application.PfmeaPort;
import com.openlab.qualitos.quality.revisionrequests.application.RevisionApplier;
import com.openlab.qualitos.quality.revisionrequests.application.RevisionAuditPort;
import com.openlab.qualitos.quality.revisionrequests.application.RevisionRequestService;
import com.openlab.qualitos.quality.revisionrequests.application.TenantProvider;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RevisionRequestsBeanConfiguration {

    @Bean
    public NcRevisionTrigger ncRevisionTrigger(NcHistoryPort history, PfmeaPort pfmea, Clock clock) {
        return new NcRevisionTrigger(history, pfmea, clock);
    }

    @Bean
    public CapaRevisionTrigger capaRevisionTrigger(NcLookupPort ncLookup, CapaActionsPort capaActions,
                                                   PfmeaPort pfmea, Clock clock) {
        return new CapaRevisionTrigger(ncLookup, capaActions, pfmea, clock);
    }

    @Bean
    public RevisionApplier revisionApplier(PfmeaPort pfmea, ControlPlanDraftPort controlPlans) {
        return new RevisionApplier(pfmea, controlPlans);
    }

    @Bean
    public RevisionRequestService revisionRequestService(
            RevisionRequestRepository repo,
            RevisionApplier applier,
            RevisionAuditPort audit,
            @Qualifier("revisionRequestTenantContextProvider") TenantProvider tenantProvider,
            @Qualifier("revisionRequestActorContextProvider") ActorProvider actorProvider,
            Clock clock) {
        return new RevisionRequestService(repo, applier, audit, tenantProvider, actorProvider, clock);
    }
}
