package com.openlab.qualitos.quality.notifications.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.notifications.domain.Notification;
import com.openlab.qualitos.quality.notifications.domain.NotificationType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditLogNotificationEventPublisherTest {

    private final AuditEventService auditEvents = mock(AuditEventService.class);
    private final AuditLogNotificationEventPublisher publisher =
            new AuditLogNotificationEventPublisher(auditEvents);

    private final UUID tenant = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-06-04T10:00:00Z");

    @Test
    void created_writesAuditEvent_broadcast() {
        Notification n = Notification.create(UUID.randomUUID(), tenant, null,
                NotificationType.INFO, "Titre", "b", "/l", now);
        publisher.created(n);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> req =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(tenant), req.capture());
        assertThat(req.getValue().action()).isEqualTo("notification.created");
        assertThat(req.getValue().resourceType()).isEqualTo("notification");
        assertThat(req.getValue().resourceId()).isEqualTo(n.getId());
        assertThat(req.getValue().actorType()).isEqualTo(ActorType.SYSTEM);
        assertThat(req.getValue().payloadJson()).contains("null");
    }

    @Test
    void created_scopedRecipient_payloadMarksScoped() {
        Notification n = Notification.create(UUID.randomUUID(), tenant, "u1",
                NotificationType.ALERT, "Titre", null, null, now);
        publisher.created(n);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> req =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(tenant), req.capture());
        assertThat(req.getValue().payloadJson()).contains("scoped");
    }
}
