package com.openlab.qualitos.quality.dpia.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.dpia.application.DpiaEventPublisher;
import com.openlab.qualitos.quality.dpia.domain.Dpia;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Adapter — route les événements DPIA vers le journal d'audit immuable.
 * Wire format : {@code gdpr.dpia.<action>}.
 * Payload : métadonnées structurelles + identité de l'auteur — pas de
 * contenu d'analyse (les champs libres restent dans l'agrégat).
 */
@Component
public class AuditLogDpiaEventPublisher implements DpiaEventPublisher {

    static final String RESOURCE_TYPE = "gdpr-dpia";

    private final AuditEventService auditEvents;

    public AuditLogDpiaEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(Dpia d, Action action) {
        String wire = "gdpr.dpia." + action.name().toLowerCase(Locale.ROOT);
        UUID actor = switch (action) {
            case APPROVED, REJECTED -> d.getDpoUserId() != null
                    ? d.getDpoUserId() : d.getCreatedByUserId();
            default -> d.getHandledByUserId() != null
                    ? d.getHandledByUserId() : d.getCreatedByUserId();
        };
        String payload = "{"
                + "\"reference\":\"" + d.getReference() + "\""
                + ",\"riskLevel\":\"" + d.getOverallRiskLevel() + "\""
                + ",\"status\":\"" + d.getStatus() + "\""
                + ",\"consultationRequired\":" + d.isConsultationRequired()
                + ",\"linkedActivities\":" + d.getLinkedProcessingActivityIds().size()
                + "}";
        String summary = action.name() + " — " + d.getReference()
                + " (risk=" + d.getOverallRiskLevel() + ")";
        auditEvents.recordForTenant(d.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, actor,
                        wire, RESOURCE_TYPE, d.getId(),
                        summary, payload, null, null));
    }
}
