package com.openlab.qualitos.quality.processoragreements.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.processoragreements.application.ProcessorAgreementEventPublisher;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreement;
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
class AuditLogProcessorAgreementEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_userAction_actorIsCreator() {
        AuditLogProcessorAgreementEventPublisher pub =
                new AuditLogProcessorAgreementEventPublisher(auditEvents);
        ProcessorAgreement a = sample();
        pub.publish(a, ProcessorAgreementEventPublisher.Action.CREATED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("gdpr.dpa.created");
        assertThat(req.resourceType()).isEqualTo("gdpr-processor-agreement");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(U);
        assertThat(req.payloadJson()).contains("\"processor\":\"Acme Corp\"");
    }

    @Test
    void publish_expired_actorIsSystem() {
        AuditLogProcessorAgreementEventPublisher pub =
                new AuditLogProcessorAgreementEventPublisher(auditEvents);
        ProcessorAgreement a = sample();
        pub.publish(a, ProcessorAgreementEventPublisher.Action.EXPIRED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("gdpr.dpa.expired");
        // EXPIRED = action automatisée → ActorType.SYSTEM, actorUserId null
        assertThat(req.actorType()).isEqualTo(ActorType.SYSTEM);
        assertThat(req.actorUserId()).isNull();
    }

    @Test
    void publish_allActions_prefixedGdprDpa() {
        AuditLogProcessorAgreementEventPublisher pub =
                new AuditLogProcessorAgreementEventPublisher(auditEvents);
        ProcessorAgreement a = sample();
        for (ProcessorAgreementEventPublisher.Action act
                : ProcessorAgreementEventPublisher.Action.values()) {
            pub.publish(a, act);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents,
                org.mockito.Mockito.times(ProcessorAgreementEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(r -> r.action().startsWith("gdpr.dpa."))
                .allMatch(r -> r.action().matches("gdpr\\.dpa\\.[a-z_]+"));
    }

    private ProcessorAgreement sample() {
        ProcessorAgreement a = ProcessorAgreement.draft(T, "DPA-2026-001",
                "Acme Corp", null, "ops@acme.com", null, "US",
                "Cloud", Set.of(), Set.of(), Set.of(), null, null,
                NOW, NOW, null, null, 72, false, null, null, U, NOW);
        a.assignId(ID);
        return a;
    }
}
