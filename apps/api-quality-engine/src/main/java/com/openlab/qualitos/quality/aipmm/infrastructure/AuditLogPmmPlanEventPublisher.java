package com.openlab.qualitos.quality.aipmm.infrastructure;

import com.openlab.qualitos.quality.aipmm.application.PmmPlanEventPublisher;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlan;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements PMM vers le journal d'audit.
 * Wire format : {@code ai.pmm.<action>}.
 */
@Component
public class AuditLogPmmPlanEventPublisher implements PmmPlanEventPublisher {

    static final String RESOURCE_TYPE = "ai-act-pmm-plan";

    private final AuditEventService auditEvents;

    public AuditLogPmmPlanEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(PmmPlan p, Action action) {
        String wire = "ai.pmm." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"reference\":\"" + p.getReference() + "\""
                + ",\"aiSystemId\":\"" + p.getAiSystemId() + "\""
                + ",\"status\":\"" + p.getStatus() + "\""
                + ",\"reviewFrequency\":"
                + (p.getReviewFrequency() != null ? "\"" + p.getReviewFrequency() + "\"" : "null")
                + "}";
        String summary = action.name() + " — " + p.getReference()
                + " (system=" + p.getAiSystemId() + ")";
        auditEvents.recordForTenant(p.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, p.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, p.getId(),
                        summary, payload, null, null));
    }
}
