package com.openlab.qualitos.quality.aiqms.infrastructure;

import com.openlab.qualitos.quality.aiqms.application.AiQmsEventPublisher;
import com.openlab.qualitos.quality.aiqms.domain.AiQms;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements QMS vers le journal d'audit.
 * Wire format : {@code ai.qms.<action>}.
 */
@Component
public class AuditLogAiQmsEventPublisher implements AiQmsEventPublisher {

    static final String RESOURCE_TYPE = "ai-act-qms";

    private final AuditEventService auditEvents;

    public AuditLogAiQmsEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(AiQms q, Action action) {
        String wire = "ai.qms." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"reference\":\"" + q.getReference() + "\""
                + ",\"version\":\"" + q.getVersion() + "\""
                + ",\"status\":\"" + q.getStatus() + "\""
                + ",\"coveredAiSystemCount\":" + q.getCoveredAiSystemIds().size()
                + "}";
        String summary = action.name() + " — " + q.getReference()
                + " v" + q.getVersion();
        auditEvents.recordForTenant(q.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, q.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, q.getId(),
                        summary, payload, null, null));
    }
}
