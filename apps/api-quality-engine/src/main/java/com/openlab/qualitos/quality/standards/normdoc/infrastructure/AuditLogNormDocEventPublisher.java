package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocEventPublisher;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Adapter — route les transitions du document normatif vers le journal d'audit
 * chaîné (OWASP A09). Wire format : {@code standards.normdoc.<action>}. Aucune
 * PII : on ne journalise que des métadonnées (code norme, type, statut).
 */
@Component
public class AuditLogNormDocEventPublisher implements NormDocEventPublisher {

    static final String RESOURCE_TYPE = "standard-norm-document";

    private final AuditEventService auditEvents;

    public AuditLogNormDocEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(NormativeDocument doc, Action action) {
        String wire = "standards.normdoc." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"standardCode\":\"" + doc.getStandardCode() + "\""
                + ",\"kind\":\"" + doc.getKind() + "\""
                + ",\"status\":\"" + doc.getStatus() + "\""
                + ",\"sectionCount\":" + doc.getSections().size()
                + "}";
        String summary = action.name() + " — " + doc.getKind() + " / " + doc.getStandardCode();
        auditEvents.recordForTenant(doc.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, actorFor(doc, action),
                        wire, RESOURCE_TYPE, doc.getId(),
                        summary, payload, null, null));
    }

    /** Acteur le plus pertinent selon la transition (sujet JWT déjà posé sur l'agrégat). */
    private static UUID actorFor(NormativeDocument doc, Action action) {
        return switch (action) {
            case APPROVED -> doc.getApprovedByUserId();
            case SUBMITTED -> doc.getSubmittedByUserId();
            default -> doc.getCreatedByUserId();
        };
    }
}
