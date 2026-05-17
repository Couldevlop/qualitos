package com.openlab.qualitos.quality.aiincidents.infrastructure;

import com.openlab.qualitos.quality.aiincidents.application.AiIncidentEventPublisher;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncident;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentSeverity;
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
class AuditLogAiIncidentEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID LEAD = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_detected_payloadCarriesSeverity() {
        AuditLogAiIncidentEventPublisher pub = new AuditLogAiIncidentEventPublisher(auditEvents);
        pub.publish(sample(false), AiIncidentEventPublisher.Action.DETECTED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("ai.incident.detected");
        assertThat(req.resourceType()).isEqualTo("ai-act-incident");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(U);
        assertThat(req.payloadJson())
                .contains("\"reference\":\"REF-1\"")
                .contains("\"severity\":\"DEATH_OR_SERIOUS_HARM_TO_HEALTH\"")
                .contains("\"notifiedRegulator\":false");
    }

    @Test
    void publish_notified_payloadFlagsTrue() {
        AuditLogAiIncidentEventPublisher pub = new AuditLogAiIncidentEventPublisher(auditEvents);
        pub.publish(sample(true), AiIncidentEventPublisher.Action.NOTIFIED_REGULATOR);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        assertThat(cap.getValue().payloadJson()).contains("\"notifiedRegulator\":true");
    }

    @Test
    void publish_allActions_prefixedAiIncident() {
        AuditLogAiIncidentEventPublisher pub = new AuditLogAiIncidentEventPublisher(auditEvents);
        AiIncident i = sample(false);
        for (AiIncidentEventPublisher.Action a : AiIncidentEventPublisher.Action.values()) {
            pub.publish(i, a);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents, times(AiIncidentEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(r -> r.action().startsWith("ai.incident."))
                .allMatch(r -> r.action().matches("ai\\.incident\\.[a-z_]+"));
    }

    private AiIncident sample(boolean notified) {
        AiIncident i = AiIncident.detect(T, "REF-1", SYS,
                AiIncidentSeverity.DEATH_OR_SERIOUS_HARM_TO_HEALTH,
                "desc", null, null,
                NOW.minusSeconds(3600), NOW, U, NOW);
        i.assignId(ID);
        if (notified) {
            i.startInvestigation(LEAD, NOW);
            i.notifyRegulator("REG-1", "rca", null, NOW);
        }
        return i;
    }
}
