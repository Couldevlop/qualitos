package com.openlab.qualitos.quality.aiconformity.infrastructure;

import com.openlab.qualitos.quality.aiconformity.application.ConformityAssessmentEventPublisher;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessment;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityProcedure;
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
class AuditLogConformityAssessmentEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final UUID QMS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-17T10:00:00Z");
    static final Instant VALID = NOW.plusSeconds(365L * 86400);

    @Test
    void publish_planned_payload() {
        AuditLogConformityAssessmentEventPublisher pub =
                new AuditLogConformityAssessmentEventPublisher(auditEvents);
        pub.publish(sample(false), ConformityAssessmentEventPublisher.Action.PLANNED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("ai.conformity.planned");
        assertThat(req.resourceType()).isEqualTo("ai-act-conformity");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.payloadJson())
                .contains("\"reference\":\"REF-1\"")
                .contains("\"procedure\":\"INTERNAL_CONTROL\"")
                .contains("\"hasNotifiedBody\":false")
                .contains("\"hasCertificate\":false");
    }

    @Test
    void publish_certified_flagsTrue() {
        AuditLogConformityAssessmentEventPublisher pub =
                new AuditLogConformityAssessmentEventPublisher(auditEvents);
        pub.publish(sample(true), ConformityAssessmentEventPublisher.Action.CERTIFIED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        assertThat(cap.getValue().payloadJson())
                .contains("\"hasNotifiedBody\":true")
                .contains("\"hasCertificate\":true");
    }

    @Test
    void publish_allActions_prefixed() {
        AuditLogConformityAssessmentEventPublisher pub =
                new AuditLogConformityAssessmentEventPublisher(auditEvents);
        ConformityAssessment a = sample(false);
        for (ConformityAssessmentEventPublisher.Action act
                : ConformityAssessmentEventPublisher.Action.values()) {
            pub.publish(a, act);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents, times(ConformityAssessmentEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(r -> r.action().startsWith("ai.conformity."))
                .allMatch(r -> r.action().matches("ai\\.conformity\\.[a-z_]+"));
    }

    private ConformityAssessment sample(boolean certified) {
        ConformityAssessment a;
        if (certified) {
            a = ConformityAssessment.plan(T, "REF-1", SYS, QMS,
                    ConformityProcedure.NOTIFIED_BODY, "1234", "TUV", "scope", U, NOW);
            a.start(NOW);
            a.certify("CERT-1", "EU-1", VALID, NOW);
        } else {
            a = ConformityAssessment.plan(T, "REF-1", SYS, QMS,
                    ConformityProcedure.INTERNAL_CONTROL, null, null, "scope", U, NOW);
        }
        a.assignId(ID);
        return a;
    }
}
