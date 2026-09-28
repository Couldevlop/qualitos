package com.openlab.qualitos.quality.costofquality.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.costofquality.application.CoqAuditPublisher;
import com.openlab.qualitos.quality.costofquality.domain.CoqEntry;

import java.util.UUID;

/**
 * Route les écritures sur le coût de la qualité vers le journal d'audit chaîné.
 *
 * <p>Aucune donnée personnelle dans la charge utile : ni le nom du responsable,
 * ni le commentaire, qui sont du texte libre. Seuls la famille, le libellé, le
 * montant, la date et le lot — de quoi rapprocher une trace d'une ligne sans
 * recopier ce que la ligne dit déjà.
 */
public class AuditLogCoqPublisher implements CoqAuditPublisher {

    static final String RESOURCE_TYPE = "coq-entry";
    static final String RECORDED = "coq.entry.recorded";
    static final String REVISED = "coq.entry.revised";
    static final String DELETED = "coq.entry.deleted";

    private final AuditEventService auditEvents;

    public AuditLogCoqPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void recorded(CoqEntry entry, UUID actor) {
        publier(RECORDED, "Coût de la qualité — ligne saisie", entry, actor);
    }

    @Override
    public void revised(CoqEntry entry, UUID actor) {
        publier(REVISED, "Coût de la qualité — ligne corrigée", entry, actor);
    }

    @Override
    public void deleted(CoqEntry entry, UUID actor) {
        publier(DELETED, "Coût de la qualité — ligne supprimée", entry, actor);
    }

    private void publier(String action, String resume, CoqEntry e, UUID actor) {
        String payload = "{"
                + "\"category\":\"" + e.getCategory() + "\""
                + ",\"labelId\":\"" + e.getLabelId() + "\""
                + ",\"amount\":" + e.getAmount().toPlainString()
                + ",\"imputationDate\":\"" + e.getImputationDate() + "\""
                + ",\"partControl\":" + e.isPartControl()
                + "}";
        auditEvents.recordForTenant(e.getTenantId(), new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, actor, action, RESOURCE_TYPE, e.getId(),
                resume, payload, null, null));
    }
}
