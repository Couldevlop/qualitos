package com.openlab.qualitos.quality.aiactfria.infrastructure;

import com.openlab.qualitos.quality.aiactfria.application.FriaEventPublisher;
import com.openlab.qualitos.quality.aiactfria.domain.Fria;
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
class AuditLogFriaEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_drafted_payloadCarriesIds() {
        AuditLogFriaEventPublisher pub = new AuditLogFriaEventPublisher(auditEvents);
        Fria f = sample(false);
        pub.publish(f, FriaEventPublisher.Action.DRAFTED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("ai.fria.drafted");
        assertThat(req.resourceType()).isEqualTo("ai-act-fria");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(U);
        assertThat(req.payloadJson())
                .contains("\"reference\":\"REF-1\"")
                .contains("\"aiSystemId\":\"" + SYS + "\"")
                .contains("\"hasMitigation\":false");
    }

    @Test
    void publish_withMitigation_payloadFlagsTrue() {
        AuditLogFriaEventPublisher pub = new AuditLogFriaEventPublisher(auditEvents);
        pub.publish(sample(true), FriaEventPublisher.Action.SUBMITTED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        assertThat(cap.getValue().payloadJson())
                .contains("\"hasMitigation\":true")
                .contains("\"hasHumanOversight\":true");
    }

    @Test
    void publish_allActions_prefixedAiFria() {
        AuditLogFriaEventPublisher pub = new AuditLogFriaEventPublisher(auditEvents);
        Fria f = sample(true);
        for (FriaEventPublisher.Action a : FriaEventPublisher.Action.values()) pub.publish(f, a);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents, times(FriaEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(r -> r.action().startsWith("ai.fria."))
                .allMatch(r -> r.action().matches("ai\\.fria\\.[a-z_]+"));
    }

    private Fria sample(boolean withMeasures) {
        Fria f = Fria.draft(T, "REF-1", SYS, "process", null, "cat", "risks",
                withMeasures ? "mitigation" : null,
                withMeasures ? "oversight" : null,
                "complaint", U, NOW);
        f.assignId(ID);
        return f;
    }
}
