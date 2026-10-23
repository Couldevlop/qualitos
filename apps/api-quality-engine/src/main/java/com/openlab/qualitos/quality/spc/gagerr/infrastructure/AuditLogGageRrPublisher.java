package com.openlab.qualitos.quality.spc.gagerr.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.spc.gagerr.application.GageRrAuditPublisher;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrStudy;

import java.util.UUID;

/**
 * Route les écritures R&amp;R vers le journal d'audit chaîné.
 *
 * <p>Aucune donnée personnelle dans la charge utile : ni le nom du responsable,
 * ni ceux des opérateurs, ni les mesures. Seuls le plan, le multiplicateur, le
 * %GRR des deux méthodes, le ndc et le verdict.
 */
public class AuditLogGageRrPublisher implements GageRrAuditPublisher {

    static final String RESOURCE = "spc-gage-rr";
    static final String RECORDED = "spc.gage-rr.recorded";
    static final String REVISED = "spc.gage-rr.revised";
    static final String DELETED = "spc.gage-rr.deleted";

    private final AuditEventService auditEvents;

    public AuditLogGageRrPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void recorded(GageRrStudy study, UUID actor) {
        publier(RECORDED, "Étude Gage R&R enregistrée", study, actor);
    }

    @Override
    public void revised(GageRrStudy study, UUID actor) {
        publier(REVISED, "Étude Gage R&R corrigée", study, actor);
    }

    @Override
    public void deleted(GageRrStudy study, UUID actor) {
        publier(DELETED, "Étude Gage R&R supprimée", study, actor);
    }

    private void publier(String action, String resume, GageRrStudy e, UUID actor) {
        String payload = "{"
                + "\"operators\":" + e.getOperatorCount()
                + ",\"parts\":" + e.getPartCount()
                + ",\"trials\":" + e.getTrialCount()
                + ",\"sigmaMultiplier\":" + e.getSigmaMultiplier()
                + ",\"grrPercent\":" + e.getGrrPercent()
                + ",\"anovaGrrPercent\":" + e.getAnovaGrrPercent()
                + ",\"ndc\":" + e.getNdc()
                + ",\"decision\":\"" + e.getDecision() + "\""
                + "}";
        auditEvents.recordForTenant(e.getTenantId(), new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, actor, action, RESOURCE, e.getId(), resume, payload, null, null));
    }
}
