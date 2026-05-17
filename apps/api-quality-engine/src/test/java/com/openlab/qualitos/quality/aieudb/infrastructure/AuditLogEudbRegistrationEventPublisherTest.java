package com.openlab.qualitos.quality.aieudb.infrastructure;

import com.openlab.qualitos.quality.aieudb.application.EudbRegistrationEventPublisher;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistration;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogEudbRegistrationEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-17T10:00:00Z");

    @Test
    void publish_drafted_payload() {
        AuditLogEudbRegistrationEventPublisher pub =
                new AuditLogEudbRegistrationEventPublisher(auditEvents);
        pub.publish(sample(false), EudbRegistrationEventPublisher.Action.DRAFTED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("ai.eudb.drafted");
        assertThat(req.resourceType()).isEqualTo("ai-act-eudb-registration");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(U);
        assertThat(req.payloadJson())
                .contains("\"reference\":\"REF-1\"")
                .contains("\"memberState\":\"FR\"")
                .contains("\"hasEudbId\":false");
    }

    @Test
    void publish_registered_flagsTrueAndIncludesEudbId() {
        AuditLogEudbRegistrationEventPublisher pub =
                new AuditLogEudbRegistrationEventPublisher(auditEvents);
        pub.publish(sample(true), EudbRegistrationEventPublisher.Action.REGISTERED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        assertThat(cap.getValue().payloadJson()).contains("\"hasEudbId\":true");
        assertThat(cap.getValue().summary()).contains("EUDB-AI-ABC123");
    }

    @Test
    void publish_nullMemberState_payloadHasNull() {
        AuditLogEudbRegistrationEventPublisher pub =
                new AuditLogEudbRegistrationEventPublisher(auditEvents);
        EudbRegistration r = EudbRegistration.draft(T, "REF-2", SYS,
                "Acme", null, null, "purpose", null, U, NOW);
        r.assignId(ID);
        pub.publish(r, EudbRegistrationEventPublisher.Action.DRAFTED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        assertThat(cap.getValue().payloadJson()).contains("\"memberState\":null");
    }

    @Test
    void publish_allActions_prefixed() {
        AuditLogEudbRegistrationEventPublisher pub =
                new AuditLogEudbRegistrationEventPublisher(auditEvents);
        EudbRegistration r = sample(false);
        for (EudbRegistrationEventPublisher.Action a
                : EudbRegistrationEventPublisher.Action.values()) {
            pub.publish(r, a);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents, times(EudbRegistrationEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(r2 -> r2.action().startsWith("ai.eudb."))
                .allMatch(r2 -> r2.action().matches("ai\\.eudb\\.[a-z_]+"));
    }

    private EudbRegistration sample(boolean registered) {
        EudbRegistration r = EudbRegistration.draft(T, "REF-1", SYS,
                "Acme", null, "FR", "purpose", null, U, NOW);
        r.assignId(ID);
        if (registered) {
            r.submit(U, NOW);
            r.markRegistered("EUDB-AI-ABC123", NOW, NOW);
        }
        return r;
    }
}
