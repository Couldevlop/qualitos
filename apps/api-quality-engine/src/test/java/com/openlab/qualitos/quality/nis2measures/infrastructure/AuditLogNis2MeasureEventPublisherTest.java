package com.openlab.qualitos.quality.nis2measures.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.nis2measures.application.Nis2MeasureEventPublisher;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureCategory;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasure;
import com.openlab.qualitos.quality.nis2measures.domain.ResidualRiskRating;
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
class AuditLogNis2MeasureEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();
    static final UUID REVIEWER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_planned_actorIsOwner() {
        AuditLogNis2MeasureEventPublisher pub = new AuditLogNis2MeasureEventPublisher(auditEvents);
        Nis2RiskMeasure m = sample();
        pub.publish(m, Nis2MeasureEventPublisher.Action.PLANNED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("nis2.measure.planned");
        assertThat(req.resourceType()).isEqualTo("nis2-risk-measure");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        // owner is set ; should be picked over creator
        assertThat(req.actorUserId()).isEqualTo(OWNER);
        assertThat(req.payloadJson()).contains("\"category\":\"CRYPTOGRAPHY\"");
    }

    @Test
    void publish_verified_actorIsReviewer() {
        AuditLogNis2MeasureEventPublisher pub = new AuditLogNis2MeasureEventPublisher(auditEvents);
        Nis2RiskMeasure m = sample();
        m.startImplementation(NOW);
        m.markImplemented(NOW);
        m.verify(REVIEWER, NOW, NOW);
        pub.publish(m, Nis2MeasureEventPublisher.Action.VERIFIED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        assertThat(cap.getValue().actorUserId()).isEqualTo(REVIEWER);
    }

    @Test
    void publish_allActions_prefixedNis2Measure() {
        AuditLogNis2MeasureEventPublisher pub = new AuditLogNis2MeasureEventPublisher(auditEvents);
        Nis2RiskMeasure m = sample();
        for (Nis2MeasureEventPublisher.Action a : Nis2MeasureEventPublisher.Action.values()) {
            pub.publish(m, a);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents,
                org.mockito.Mockito.times(Nis2MeasureEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(req -> req.action().startsWith("nis2.measure."))
                .allMatch(req -> req.action().matches("nis2\\.measure\\.[a-z_]+"));
    }

    private Nis2RiskMeasure sample() {
        Nis2RiskMeasure m = Nis2RiskMeasure.plan(T, "M-2026-001",
                Nis2MeasureCategory.CRYPTOGRAPHY, "AES-256 at rest", null,
                OWNER, 3, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, U, NOW);
        m.assignId(ID);
        return m;
    }
}
