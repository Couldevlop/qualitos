package com.openlab.qualitos.quality.aiactfria.infrastructure;

import com.openlab.qualitos.quality.aiactfria.application.FriaEventPublisher;
import com.openlab.qualitos.quality.aiactfria.domain.Fria;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements FRIA vers le journal d'audit.
 * Wire format : {@code ai.fria.<action>}.
 */
@Component
public class AuditLogFriaEventPublisher implements FriaEventPublisher {

    static final String RESOURCE_TYPE = "ai-act-fria";

    private final AuditEventService auditEvents;

    public AuditLogFriaEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(Fria f, Action action) {
        String wire = "ai.fria." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"reference\":\"" + f.getReference() + "\""
                + ",\"aiSystemId\":\"" + f.getAiSystemId() + "\""
                + ",\"status\":\"" + f.getStatus() + "\""
                + ",\"hasMitigation\":" + (f.getMitigationMeasures() != null
                        && !f.getMitigationMeasures().isBlank())
                + ",\"hasHumanOversight\":" + (f.getHumanOversightMeasures() != null
                        && !f.getHumanOversightMeasures().isBlank())
                + "}";
        String summary = action.name() + " — " + f.getReference()
                + " (system=" + f.getAiSystemId() + ")";
        auditEvents.recordForTenant(f.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, f.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, f.getId(),
                        summary, payload, null, null));
    }
}
