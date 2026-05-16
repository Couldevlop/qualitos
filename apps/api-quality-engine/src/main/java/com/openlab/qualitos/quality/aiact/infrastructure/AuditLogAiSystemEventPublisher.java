package com.openlab.qualitos.quality.aiact.infrastructure;

import com.openlab.qualitos.quality.aiact.application.AiSystemEventPublisher;
import com.openlab.qualitos.quality.aiact.domain.AiSystem;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements de système IA vers le journal d'audit.
 * Wire format : {@code ai.system.<action>}.
 */
@Component
public class AuditLogAiSystemEventPublisher implements AiSystemEventPublisher {

    static final String RESOURCE_TYPE = "ai-act-system";

    private final AuditEventService auditEvents;

    public AuditLogAiSystemEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(AiSystem s, Action action) {
        String wire = "ai.system." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"reference\":\"" + s.getReference() + "\""
                + ",\"risk\":\"" + s.getRiskClassification() + "\""
                + ",\"role\":\"" + s.getRole() + "\""
                + ",\"generalPurpose\":" + s.isGeneralPurpose()
                + ",\"status\":\"" + s.getStatus() + "\""
                + ",\"hasLinkedDpia\":" + (s.getLinkedDpiaId() != null)
                + "}";
        String summary = action.name() + " — " + s.getReference()
                + " (" + s.getRiskClassification() + "/" + s.getRole() + ")";
        auditEvents.recordForTenant(s.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, s.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, s.getId(),
                        summary, payload, null, null));
    }
}
