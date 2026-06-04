package com.openlab.qualitos.quality.notifications.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.notifications.application.NotificationEventPublisher;
import com.openlab.qualitos.quality.notifications.domain.Notification;
import org.springframework.stereotype.Component;

/** Trace la création de notification dans le journal d'audit immuable (OWASP A09). */
@Component
public class AuditLogNotificationEventPublisher implements NotificationEventPublisher {

    static final String RESOURCE_TYPE = "notification";

    private final AuditEventService auditEvents;

    public AuditLogNotificationEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void created(Notification n) {
        String summary = "Notification " + n.getType() + " — " + n.getTitle();
        String payload = "{\"type\":\"" + n.getType()
                + "\",\"recipient\":" + (n.getRecipientUserId() == null ? "null" : "\"scoped\"") + "}";
        auditEvents.recordForTenant(n.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.SYSTEM, null,
                        "notification.created", RESOURCE_TYPE, n.getId(),
                        summary, payload, null, null));
    }
}
