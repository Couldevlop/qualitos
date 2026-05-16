package com.openlab.qualitos.quality.automateddecisions.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.automateddecisions.application.AutomatedDecisionEventPublisher;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRecord;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionType;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogAutomatedDecisionEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_created_actorIsCreator_payloadHasMetadata() {
        AuditLogAutomatedDecisionEventPublisher pub =
                new AuditLogAutomatedDecisionEventPublisher(auditEvents);
        AutomatedDecisionRecord r = sample();
        pub.publish(r, AutomatedDecisionEventPublisher.Action.CREATED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("gdpr.adm.created");
        assertThat(req.resourceType()).isEqualTo("gdpr-automated-decision");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(U);
        assertThat(req.payloadJson())
                .contains("\"decisionType\":\"PROFILING_ONLY\"")
                .contains("\"hasLinkedDpia\":false");
    }

    @Test
    void publish_allActions_prefixedGdprAdm() {
        AuditLogAutomatedDecisionEventPublisher pub =
                new AuditLogAutomatedDecisionEventPublisher(auditEvents);
        AutomatedDecisionRecord r = sample();
        for (AutomatedDecisionEventPublisher.Action a
                : AutomatedDecisionEventPublisher.Action.values()) {
            pub.publish(r, a);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents,
                org.mockito.Mockito.times(AutomatedDecisionEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(req -> req.action().startsWith("gdpr.adm."))
                .allMatch(req -> req.action().matches("gdpr\\.adm\\.[a-z_]+"));
    }

    private AutomatedDecisionRecord sample() {
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(T, "ADM-2026-001",
                "Recommandations", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null,
                null, null, null, null, U, NOW);
        r.assignId(ID);
        return r;
    }
}
