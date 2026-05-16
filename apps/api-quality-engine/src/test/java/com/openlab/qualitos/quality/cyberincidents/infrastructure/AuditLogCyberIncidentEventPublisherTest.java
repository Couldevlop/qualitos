package com.openlab.qualitos.quality.cyberincidents.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.cyberincidents.application.CyberIncidentEventPublisher;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncident;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentSeverity;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentType;
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
class AuditLogCyberIncidentEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_detected_payloadHasMetadata() {
        AuditLogCyberIncidentEventPublisher pub =
                new AuditLogCyberIncidentEventPublisher(auditEvents);
        CyberIncident i = sample();
        pub.publish(i, CyberIncidentEventPublisher.Action.DETECTED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("nis2.cyber.detected");
        assertThat(req.resourceType()).isEqualTo("nis2-cyber-incident");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.payloadJson())
                .contains("\"type\":\"RANSOMWARE\"")
                .contains("\"severity\":\"HIGH\"")
                .contains("\"linkedToGdprBreach\":false");
    }

    @Test
    void publish_allActions_prefixedNis2Cyber() {
        AuditLogCyberIncidentEventPublisher pub =
                new AuditLogCyberIncidentEventPublisher(auditEvents);
        CyberIncident i = sample();
        for (CyberIncidentEventPublisher.Action a : CyberIncidentEventPublisher.Action.values()) {
            pub.publish(i, a);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents,
                org.mockito.Mockito.times(CyberIncidentEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(req -> req.action().startsWith("nis2.cyber."))
                .allMatch(req -> req.action().matches("nis2\\.cyber\\.[a-z_]+"));
    }

    private CyberIncident sample() {
        CyberIncident i = CyberIncident.detect(T, "CYB-2026-001", "Ransomware",
                "EDR alert", NOW, null,
                CyberIncidentType.RANSOMWARE, CyberIncidentSeverity.HIGH,
                500L, Set.of(), Set.of(), null, U);
        i.assignId(ID);
        return i;
    }
}
