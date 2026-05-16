package com.openlab.qualitos.quality.retention.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.retention.application.RetentionRuleEventPublisher;
import com.openlab.qualitos.quality.retention.domain.RetentionRule;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements de règle de rétention vers le journal d'audit.
 * Wire format : {@code gdpr.retention.<action>}.
 */
@Component
public class AuditLogRetentionRuleEventPublisher implements RetentionRuleEventPublisher {

    static final String RESOURCE_TYPE = "gdpr-retention-rule";

    private final AuditEventService auditEvents;

    public AuditLogRetentionRuleEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(RetentionRule r, Action action) {
        String wire = "gdpr.retention." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"category\":\"" + r.getDataCategoryCode() + "\""
                + ",\"periodSeconds\":" + r.getRetentionPeriod().getSeconds()
                + ",\"status\":\"" + r.getStatus() + "\""
                + "}";
        String summary = action.name() + " — category=" + r.getDataCategoryCode()
                + " period=" + r.getRetentionPeriod();
        auditEvents.recordForTenant(r.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, r.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, r.getId(),
                        summary, payload, null, null));
    }
}
