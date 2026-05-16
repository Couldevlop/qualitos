package com.openlab.qualitos.quality.ehs.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.ehs.application.IncidentEventPublisher;
import com.openlab.qualitos.quality.ehs.domain.Incident;
import com.openlab.qualitos.quality.ehs.domain.IncidentSeverity;
import com.openlab.qualitos.quality.ehs.domain.IncidentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogIncidentEventPublisherTest {

    @Mock AuditEventService auditEvents;

    @Test
    void publish_routesToAuditLogWithDottedAction() {
        AuditLogIncidentEventPublisher pub = new AuditLogIncidentEventPublisher(auditEvents);
        UUID tenantId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        Incident i = sample(tenantId, owner);
        i.assignId(incidentId);

        pub.publish(i, IncidentEventPublisher.Action.MITIGATED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(tenantId), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("ehs.incident.mitigated");
        assertThat(req.resourceType()).isEqualTo("ehs-incident");
        assertThat(req.resourceId()).isEqualTo(incidentId);
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(owner);
        assertThat(req.summary()).contains("EHS-1").contains("Fall");
    }

    @Test
    void publish_eachActionMapsToDottedWire() {
        AuditLogIncidentEventPublisher pub = new AuditLogIncidentEventPublisher(auditEvents);
        Incident i = sample(UUID.randomUUID(), null);
        i.assignId(UUID.randomUUID());
        for (IncidentEventPublisher.Action a : IncidentEventPublisher.Action.values()) {
            pub.publish(i, a);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents, org.mockito.Mockito.times(IncidentEventPublisher.Action.values().length))
                .recordForTenant(any(), cap.capture());
        // Toutes les actions doivent commencer par "ehs.incident."
        assertThat(cap.getAllValues())
                .allMatch(r -> r.action().startsWith("ehs.incident."))
                .allMatch(r -> r.action().matches("ehs\\.incident\\.[a-z_]+"));
    }

    private Incident sample(UUID tenantId, UUID ownerUserId) {
        Incident i = Incident.report(tenantId, "EHS-1", "Fall", null,
                IncidentType.INJURY, IncidentSeverity.MEDIUM,
                Instant.parse("2026-05-15T10:00:00Z"), "Loc",
                UUID.randomUUID(), Instant.parse("2026-05-15T10:00:00Z"));
        if (ownerUserId != null) {
            i.investigate(ownerUserId, Instant.parse("2026-05-15T11:00:00Z"));
        }
        return i;
    }
}
