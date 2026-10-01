package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.riskregister.application.RegisterAuditPublisher;
import com.openlab.qualitos.quality.riskregister.domain.Opportunity;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityAction;
import com.openlab.qualitos.quality.riskregister.domain.Risk;

import java.util.UUID;

/**
 * Route les écritures du registre vers le journal d'audit chaîné.
 *
 * <p>Aucun texte libre dans la charge utile : ni intitulé, ni cause, ni nom de
 * propriétaire — ce sont des données saisies, parfois personnelles, et le
 * journal est immuable. Seuls la référence, le type, les notes, la décision et
 * le statut, de quoi rapprocher une trace de sa fiche et voir ce qui a bougé.
 * Toutes les valeurs sont des énumérations ou des entiers : rien ne peut
 * casser le JSON.
 */
public class AuditLogRegisterPublisher implements RegisterAuditPublisher {

    static final String RISK_RESOURCE = "register-risk";
    static final String OPPORTUNITY_RESOURCE = "register-opportunity";
    static final String ACTION_RESOURCE = "register-action";

    static final String RISK_RECORDED = "register.risk.recorded";
    static final String RISK_REVISED = "register.risk.revised";
    static final String RISK_CAPA_OPENED = "register.risk.capa-opened";
    static final String OPPORTUNITY_RECORDED = "register.opportunity.recorded";
    static final String OPPORTUNITY_REVISED = "register.opportunity.revised";
    static final String ACTION_RECORDED = "register.action.recorded";
    static final String ACTION_REVISED = "register.action.revised";
    static final String ACTION_DELETED = "register.action.deleted";

    private final AuditEventService auditEvents;

    public AuditLogRegisterPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void riskRecorded(Risk risk, UUID actor) {
        publier(risk.getTenantId(), RISK_RECORDED, RISK_RESOURCE, risk.getId(),
                "Registre des risques — risque saisi", risque(risk, null), actor);
    }

    @Override
    public void riskRevised(Risk risk, UUID actor) {
        publier(risk.getTenantId(), RISK_REVISED, RISK_RESOURCE, risk.getId(),
                "Registre des risques — risque révisé", risque(risk, null), actor);
    }

    @Override
    public void riskCapaOpened(Risk risk, UUID capaId, UUID actor) {
        publier(risk.getTenantId(), RISK_CAPA_OPENED, RISK_RESOURCE, risk.getId(),
                "Registre des risques — action CAPA ouverte", risque(risk, capaId), actor);
    }

    @Override
    public void opportunityRecorded(Opportunity o, UUID actor) {
        publier(o.getTenantId(), OPPORTUNITY_RECORDED, OPPORTUNITY_RESOURCE, o.getId(),
                "Registre des opportunités — opportunité saisie", opportunite(o), actor);
    }

    @Override
    public void opportunityRevised(Opportunity o, UUID actor) {
        publier(o.getTenantId(), OPPORTUNITY_REVISED, OPPORTUNITY_RESOURCE, o.getId(),
                "Registre des opportunités — opportunité révisée", opportunite(o), actor);
    }

    @Override
    public void actionRecorded(OpportunityAction a, UUID actor) {
        publier(a.getTenantId(), ACTION_RECORDED, ACTION_RESOURCE, a.getId(),
                "Registre des opportunités — action ouverte", action(a), actor);
    }

    @Override
    public void actionRevised(OpportunityAction a, UUID actor) {
        publier(a.getTenantId(), ACTION_REVISED, ACTION_RESOURCE, a.getId(),
                "Registre des opportunités — action révisée", action(a), actor);
    }

    @Override
    public void actionDeleted(OpportunityAction a, UUID actor) {
        publier(a.getTenantId(), ACTION_DELETED, ACTION_RESOURCE, a.getId(),
                "Registre des opportunités — action supprimée", action(a), actor);
    }

    private static String risque(Risk r, UUID capaId) {
        return "{"
                + "\"reference\":\"" + r.getReference() + "\""
                + ",\"type\":\"" + r.getIdentification().type() + "\""
                + ",\"gross\":\"" + r.getGross().code() + "\""
                + ",\"residual\":" + (r.getResidual() == null ? "null" : "\"" + r.getResidual().code() + "\"")
                + ",\"decision\":\"" + r.getDecision() + "\""
                + ",\"status\":\"" + r.getStatus() + "\""
                + (capaId == null ? "" : ",\"capaId\":\"" + capaId + "\"")
                + "}";
    }

    private static String opportunite(Opportunity o) {
        return "{"
                + "\"reference\":\"" + o.getReference() + "\""
                + ",\"type\":\"" + o.getIdentification().type() + "\""
                + ",\"evaluation\":\"" + o.getEvaluation().code() + "\""
                + ",\"decision\":\"" + o.getDecision() + "\""
                + ",\"status\":\"" + o.getStatus() + "\""
                + "}";
    }

    private static String action(OpportunityAction a) {
        return "{"
                + "\"opportunityId\":\"" + a.getOpportunityId() + "\""
                + ",\"number\":" + a.getNumber()
                + ",\"status\":\"" + a.getStatus() + "\""
                + "}";
    }

    private void publier(UUID tenant, String action, String resourceType, UUID resourceId,
                         String resume, String payload, UUID actor) {
        auditEvents.recordForTenant(tenant, new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, actor, action, resourceType, resourceId,
                resume, payload, null, null));
    }
}
