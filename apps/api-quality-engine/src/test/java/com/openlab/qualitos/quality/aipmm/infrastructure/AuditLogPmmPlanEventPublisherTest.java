package com.openlab.qualitos.quality.aipmm.infrastructure;

import com.openlab.qualitos.quality.aipmm.application.PmmPlanEventPublisher;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlan;
import com.openlab.qualitos.quality.aipmm.domain.PmmReviewFrequency;
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
class AuditLogPmmPlanEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_drafted_payloadCarriesIds() {
        AuditLogPmmPlanEventPublisher pub = new AuditLogPmmPlanEventPublisher(auditEvents);
        pub.publish(sample(PmmReviewFrequency.MONTHLY), PmmPlanEventPublisher.Action.DRAFTED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("ai.pmm.drafted");
        assertThat(req.resourceType()).isEqualTo("ai-act-pmm-plan");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(U);
        assertThat(req.payloadJson())
                .contains("\"reference\":\"REF-1\"")
                .contains("\"aiSystemId\":\"" + SYS + "\"")
                .contains("\"reviewFrequency\":\"MONTHLY\"");
    }

    @Test
    void publish_nullFrequency_payloadHasNull() {
        AuditLogPmmPlanEventPublisher pub = new AuditLogPmmPlanEventPublisher(auditEvents);
        pub.publish(sample(null), PmmPlanEventPublisher.Action.DRAFTED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        assertThat(cap.getValue().payloadJson()).contains("\"reviewFrequency\":null");
    }

    @Test
    void publish_allActions_prefixedAiPmm() {
        AuditLogPmmPlanEventPublisher pub = new AuditLogPmmPlanEventPublisher(auditEvents);
        PmmPlan p = sample(PmmReviewFrequency.MONTHLY);
        for (PmmPlanEventPublisher.Action a : PmmPlanEventPublisher.Action.values()) {
            pub.publish(p, a);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents, times(PmmPlanEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(r -> r.action().startsWith("ai.pmm."))
                .allMatch(r -> r.action().matches("ai\\.pmm\\.[a-z_]+"));
    }

    private PmmPlan sample(PmmReviewFrequency freq) {
        PmmPlan p = PmmPlan.draft(T, "REF-1", SYS, "Name", null,
                "metrics", "method", freq, null, null, null, U, NOW);
        p.assignId(ID);
        return p;
    }
}
