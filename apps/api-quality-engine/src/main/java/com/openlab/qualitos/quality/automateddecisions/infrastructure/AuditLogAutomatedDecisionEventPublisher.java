package com.openlab.qualitos.quality.automateddecisions.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.automateddecisions.application.AutomatedDecisionEventPublisher;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRecord;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements de décisions automatisées vers le journal
 * d'audit immuable. Wire format : {@code gdpr.adm.<action>}.
 */
@Component
public class AuditLogAutomatedDecisionEventPublisher implements AutomatedDecisionEventPublisher {

    static final String RESOURCE_TYPE = "gdpr-automated-decision";

    private final AuditEventService auditEvents;

    public AuditLogAutomatedDecisionEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(AutomatedDecisionRecord r, Action action) {
        String wire = "gdpr.adm." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"reference\":\"" + r.getReference() + "\""
                + ",\"decisionType\":\"" + r.getDecisionType() + "\""
                + ",\"status\":\"" + r.getStatus() + "\""
                + ",\"hasLinkedDpia\":" + (r.getLinkedDpiaId() != null)
                + ",\"linkedActivities\":" + r.getLinkedProcessingActivityIds().size()
                + "}";
        String summary = action.name() + " — " + r.getReference()
                + " (" + r.getDecisionType() + ")";
        auditEvents.recordForTenant(r.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, r.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, r.getId(),
                        summary, payload, null, null));
    }
}
