package com.openlab.qualitos.quality.aiact.infrastructure;

import com.openlab.qualitos.quality.aiact.application.AiSystemEventPublisher;
import com.openlab.qualitos.quality.aiact.domain.AiRiskClassification;
import com.openlab.qualitos.quality.aiact.domain.AiSystem;
import com.openlab.qualitos.quality.aiact.domain.AiSystemRole;
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
class AuditLogAiSystemEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID DPIA_ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_drafted_payloadCarriesClassification() {
        AuditLogAiSystemEventPublisher pub = new AuditLogAiSystemEventPublisher(auditEvents);
        AiSystem s = sample(AiRiskClassification.HIGH, DPIA_ID);
        pub.publish(s, AiSystemEventPublisher.Action.DRAFTED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("ai.system.drafted");
        assertThat(req.resourceType()).isEqualTo("ai-act-system");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(U);
        assertThat(req.payloadJson())
                .contains("\"reference\":\"REF-1\"")
                .contains("\"risk\":\"HIGH\"")
                .contains("\"role\":\"PROVIDER\"")
                .contains("\"hasLinkedDpia\":true");
    }

    @Test
    void publish_noLinkedDpia_payloadHasFalse() {
        AuditLogAiSystemEventPublisher pub = new AuditLogAiSystemEventPublisher(auditEvents);
        AiSystem s = sample(AiRiskClassification.LIMITED, null);
        pub.publish(s, AiSystemEventPublisher.Action.DRAFTED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        assertThat(cap.getValue().payloadJson()).contains("\"hasLinkedDpia\":false");
    }

    @Test
    void publish_allActions_prefixedAiSystem() {
        AuditLogAiSystemEventPublisher pub = new AuditLogAiSystemEventPublisher(auditEvents);
        AiSystem s = sample(AiRiskClassification.MINIMAL_OR_NO, null);
        for (AiSystemEventPublisher.Action act : AiSystemEventPublisher.Action.values()) {
            pub.publish(s, act);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents, times(AiSystemEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(r -> r.action().startsWith("ai.system."))
                .allMatch(r -> r.action().matches("ai\\.system\\.[a-z_]+"));
    }

    private AiSystem sample(AiRiskClassification risk, UUID dpiaId) {
        AiSystem s = AiSystem.draft(T, "REF-1", "Name", "desc", "Provider", "purpose",
                risk, AiSystemRole.PROVIDER, false,
                null, null, null, null, null,
                dpiaId, null, null, U, NOW);
        s.assignId(ID);
        return s;
    }
}
