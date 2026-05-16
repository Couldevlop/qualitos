package com.openlab.qualitos.quality.dpoappointments.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.dpoappointments.application.DpoAppointmentEventPublisher;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointment;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoType;
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
class AuditLogDpoAppointmentEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_proposed_payloadHasMetadata_noPiiInClear() {
        AuditLogDpoAppointmentEventPublisher pub =
                new AuditLogDpoAppointmentEventPublisher(auditEvents);
        DpoAppointment a = sample();
        pub.publish(a, DpoAppointmentEventPublisher.Action.PROPOSED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("gdpr.dpo.proposed");
        assertThat(req.resourceType()).isEqualTo("gdpr-dpo-appointment");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(U);
        // OWASP A02 : pas d'email/téléphone DPO dans le payload audit.
        assertThat(req.payloadJson())
                .doesNotContain("dpo@example.com")
                .contains("\"dpoType\":\"INTERNAL\"")
                .contains("\"scope\":\"GROUP\"");
    }

    @Test
    void publish_allActions_prefixedGdprDpo() {
        AuditLogDpoAppointmentEventPublisher pub =
                new AuditLogDpoAppointmentEventPublisher(auditEvents);
        DpoAppointment a = sample();
        for (DpoAppointmentEventPublisher.Action act
                : DpoAppointmentEventPublisher.Action.values()) {
            pub.publish(a, act);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents,
                org.mockito.Mockito.times(DpoAppointmentEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(req -> req.action().startsWith("gdpr.dpo."))
                .allMatch(req -> req.action().matches("gdpr\\.dpo\\.[a-z_]+"));
    }

    private DpoAppointment sample() {
        DpoAppointment a = DpoAppointment.propose(T, "DPO-2026-001",
                "Jane Doe", "dpo@example.com", null,
                DpoType.INTERNAL, null, null, "GROUP", Set.of(), U, NOW);
        a.assignId(ID);
        return a;
    }
}
