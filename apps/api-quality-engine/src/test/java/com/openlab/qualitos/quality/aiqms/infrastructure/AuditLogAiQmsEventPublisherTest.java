package com.openlab.qualitos.quality.aiqms.infrastructure;

import com.openlab.qualitos.quality.aiqms.application.AiQmsEventPublisher;
import com.openlab.qualitos.quality.aiqms.domain.AiQms;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogAiQmsEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_drafted_payloadCarriesVersionAndCount() {
        AuditLogAiQmsEventPublisher pub = new AuditLogAiQmsEventPublisher(auditEvents);
        pub.publish(sample(Set.of(SYS)), AiQmsEventPublisher.Action.DRAFTED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("ai.qms.drafted");
        assertThat(req.resourceType()).isEqualTo("ai-act-qms");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(U);
        assertThat(req.payloadJson())
                .contains("\"reference\":\"REF-1\"")
                .contains("\"version\":\"1.0\"")
                .contains("\"coveredAiSystemCount\":1");
    }

    @Test
    void publish_emptyCovered_count0() {
        AuditLogAiQmsEventPublisher pub = new AuditLogAiQmsEventPublisher(auditEvents);
        pub.publish(sample(Set.of()), AiQmsEventPublisher.Action.DRAFTED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        assertThat(cap.getValue().payloadJson()).contains("\"coveredAiSystemCount\":0");
    }

    @Test
    void publish_allActions_prefixedAiQms() {
        AuditLogAiQmsEventPublisher pub = new AuditLogAiQmsEventPublisher(auditEvents);
        AiQms q = sample(Set.of());
        for (AiQmsEventPublisher.Action a : AiQmsEventPublisher.Action.values()) {
            pub.publish(q, a);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents, times(AiQmsEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(r -> r.action().startsWith("ai.qms."))
                .allMatch(r -> r.action().matches("ai\\.qms\\.[a-z_]+"));
    }

    private AiQms sample(Set<UUID> covered) {
        AiQms q = AiQms.draft(T, "REF-1", "1.0", "Name", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, covered, U, NOW);
        q.assignId(ID);
        return q;
    }
}
