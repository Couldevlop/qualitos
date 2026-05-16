package com.openlab.qualitos.quality.dpia.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.dpia.application.DpiaEventPublisher;
import com.openlab.qualitos.quality.dpia.domain.Dpia;
import com.openlab.qualitos.quality.dpia.domain.RiskLevel;
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
class AuditLogDpiaEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID DPO = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_actionsBeyondApproveReject_useHandlerOrCreator() {
        AuditLogDpiaEventPublisher pub = new AuditLogDpiaEventPublisher(auditEvents);
        Dpia d = Dpia.draft(T, "DPIA-1", "t", null,
                Set.of(), RiskLevel.LOW, U, NOW);
        d.assignId(ID);

        pub.publish(d, DpiaEventPublisher.Action.CREATED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("gdpr.dpia.created");
        assertThat(req.resourceType()).isEqualTo("gdpr-dpia");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        // Pas de handler — fallback sur createdBy.
        assertThat(req.actorUserId()).isEqualTo(U);
        assertThat(req.summary()).contains("CREATED").contains("DPIA-1");
        assertThat(req.payloadJson()).contains("\"riskLevel\":\"LOW\"");
    }

    @Test
    void publish_approve_usesDpoUserAsActor() {
        AuditLogDpiaEventPublisher pub = new AuditLogDpiaEventPublisher(auditEvents);
        Dpia d = Dpia.draft(T, "DPIA-2", "t", null,
                Set.of(), RiskLevel.LOW, U, NOW);
        d.assignId(ID);
        d.editDraft("t", null, Set.of(),
                "necessity", "risks", "mitigations",
                RiskLevel.LOW, false, null, NOW);
        d.start(U, NOW);
        d.submitToDpo(NOW);
        d.approve(DPO, "ok", NOW);

        pub.publish(d, DpiaEventPublisher.Action.APPROVED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("gdpr.dpia.approved");
        // Action APPROVED → switch case sélectionne dpoUserId.
        assertThat(req.actorUserId()).isEqualTo(DPO);
    }

    @Test
    void publish_handlerSet_takesPrecedenceOverCreator() {
        AuditLogDpiaEventPublisher pub = new AuditLogDpiaEventPublisher(auditEvents);
        UUID handler = UUID.randomUUID();
        Dpia d = Dpia.draft(T, "DPIA-3", "t", null, Set.of(), RiskLevel.LOW, U, NOW);
        d.assignId(ID);
        d.start(handler, NOW);

        pub.publish(d, DpiaEventPublisher.Action.STARTED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        assertThat(cap.getValue().actorUserId()).isEqualTo(handler);
    }

    @Test
    void publish_allActionsPrefixed_gdpr_dpia() {
        AuditLogDpiaEventPublisher pub = new AuditLogDpiaEventPublisher(auditEvents);
        Dpia d = Dpia.draft(T, "DPIA-4", "t", null, Set.of(), RiskLevel.LOW, U, NOW);
        d.assignId(ID);
        for (DpiaEventPublisher.Action a : DpiaEventPublisher.Action.values()) {
            pub.publish(d, a);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents, org.mockito.Mockito.times(DpiaEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(r -> r.action().startsWith("gdpr.dpia."))
                .allMatch(r -> r.action().matches("gdpr\\.dpia\\.[a-z_]+"));
    }
}
