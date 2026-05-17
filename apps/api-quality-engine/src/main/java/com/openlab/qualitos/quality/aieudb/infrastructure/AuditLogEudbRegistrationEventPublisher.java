package com.openlab.qualitos.quality.aieudb.infrastructure;

import com.openlab.qualitos.quality.aieudb.application.EudbRegistrationEventPublisher;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistration;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements EUDB vers le journal d'audit.
 * Wire format : {@code ai.eudb.<action>}.
 */
@Component
public class AuditLogEudbRegistrationEventPublisher implements EudbRegistrationEventPublisher {

    static final String RESOURCE_TYPE = "ai-act-eudb-registration";

    private final AuditEventService auditEvents;

    public AuditLogEudbRegistrationEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(EudbRegistration r, Action action) {
        String wire = "ai.eudb." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"reference\":\"" + r.getReference() + "\""
                + ",\"aiSystemId\":\"" + r.getAiSystemId() + "\""
                + ",\"memberState\":"
                + (r.getMemberStateOfReference() != null
                        ? "\"" + r.getMemberStateOfReference() + "\"" : "null")
                + ",\"status\":\"" + r.getStatus() + "\""
                + ",\"hasEudbId\":" + (r.getEudbId() != null)
                + "}";
        String summary = action.name() + " — " + r.getReference()
                + (r.getEudbId() != null ? " [" + r.getEudbId() + "]" : "");
        auditEvents.recordForTenant(r.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, r.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, r.getId(),
                        summary, payload, null, null));
    }
}
