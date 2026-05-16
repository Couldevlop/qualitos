package com.openlab.qualitos.quality.aiincidents.infrastructure;

import com.openlab.qualitos.quality.aiincidents.application.AiIncidentEventPublisher;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncident;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements d'incidents IA vers le journal d'audit.
 * Wire format : {@code ai.incident.<action>}.
 */
@Component
public class AuditLogAiIncidentEventPublisher implements AiIncidentEventPublisher {

    static final String RESOURCE_TYPE = "ai-act-incident";

    private final AuditEventService auditEvents;

    public AuditLogAiIncidentEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(AiIncident i, Action action) {
        String wire = "ai.incident." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"reference\":\"" + i.getReference() + "\""
                + ",\"aiSystemId\":\"" + i.getAiSystemId() + "\""
                + ",\"severity\":\"" + i.getSeverity() + "\""
                + ",\"status\":\"" + i.getStatus() + "\""
                + ",\"notifiedRegulator\":" + (i.getNotifiedRegulatorAt() != null)
                + "}";
        String summary = action.name() + " — " + i.getReference()
                + " (" + i.getSeverity() + ")";
        auditEvents.recordForTenant(i.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, i.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, i.getId(),
                        summary, payload, null, null));
    }
}
