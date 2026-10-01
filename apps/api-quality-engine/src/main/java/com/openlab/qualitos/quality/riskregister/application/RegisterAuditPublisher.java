package com.openlab.qualitos.quality.riskregister.application;

import com.openlab.qualitos.quality.riskregister.domain.Opportunity;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityAction;
import com.openlab.qualitos.quality.riskregister.domain.Risk;

import java.util.UUID;

/**
 * Port d'audit (OWASP A09) : toute écriture sur le registre laisse une trace
 * dans le journal chaîné. Une cotation abaissée la veille d'un audit doit
 * pouvoir se retrouver, avec son auteur.
 */
public interface RegisterAuditPublisher {

    void riskRecorded(Risk risk, UUID actor);

    void riskRevised(Risk risk, UUID actor);

    void riskCapaOpened(Risk risk, UUID capaId, UUID actor);

    void opportunityRecorded(Opportunity opportunity, UUID actor);

    void opportunityRevised(Opportunity opportunity, UUID actor);

    void actionRecorded(OpportunityAction action, UUID actor);

    void actionRevised(OpportunityAction action, UUID actor);

    void actionDeleted(OpportunityAction action, UUID actor);
}
