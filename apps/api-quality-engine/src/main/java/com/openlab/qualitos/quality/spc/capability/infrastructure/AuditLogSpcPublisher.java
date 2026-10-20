package com.openlab.qualitos.quality.spc.capability.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.spc.capability.application.SpcAuditPublisher;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStudy;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityThresholds;

import java.util.UUID;

/**
 * Route les écritures SPC vers le journal d'audit chaîné.
 *
 * <p>Aucune donnée personnelle dans la charge utile : ni le nom du meneur ni la
 * grille de mesures. Seuls la taille du sous-groupe, Cp, Cpk, le verdict et le
 * plan d'actions — de quoi rapprocher une trace d'une étude, et voir qu'un Cpk a
 * changé, sans recopier ce que l'étude dit déjà.
 */
public class AuditLogSpcPublisher implements SpcAuditPublisher {

    static final String STUDY = "spc-study";
    static final String SETTINGS = "spc-settings";
    static final String RECORDED = "spc.study.recorded";
    static final String REVISED = "spc.study.revised";
    static final String ACTION_PLAN = "spc.study.action-plan";
    static final String DELETED = "spc.study.deleted";
    static final String THRESHOLDS = "spc.settings.updated";

    private final AuditEventService auditEvents;

    public AuditLogSpcPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void recorded(CapabilityStudy study, UUID actor) {
        publier(RECORDED, "Étude SPC enregistrée", study, actor);
    }

    @Override
    public void revised(CapabilityStudy study, UUID actor) {
        publier(REVISED, "Étude SPC corrigée", study, actor);
    }

    @Override
    public void actionPlanChanged(CapabilityStudy study, UUID actor) {
        publier(ACTION_PLAN, "Étude SPC — plan d'actions modifié", study, actor);
    }

    @Override
    public void deleted(CapabilityStudy study, UUID actor) {
        publier(DELETED, "Étude SPC supprimée", study, actor);
    }

    @Override
    public void thresholdsChanged(UUID tenantId, CapabilityThresholds thresholds, UUID actor) {
        String payload = "{\"capableThreshold\":" + thresholds.capable().toPlainString()
                + ",\"watchThreshold\":" + thresholds.watch().toPlainString() + "}";
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, actor, THRESHOLDS, SETTINGS, tenantId,
                "Seuils de capabilité SPC modifiés", payload, null, null));
    }

    private void publier(String action, String resume, CapabilityStudy e, UUID actor) {
        String payload = "{"
                + "\"subgroupSize\":" + e.getSubgroupSize()
                + ",\"cp\":" + e.getCp()
                + ",\"cpk\":" + e.getCpk()
                + ",\"status\":\"" + e.status() + "\""
                + ",\"actionPlan\":" + e.isActionPlan()
                + "}";
        auditEvents.recordForTenant(e.getTenantId(), new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, actor, action, STUDY, e.getId(), resume, payload, null, null));
    }
}
