package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierEventPublisher;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DocumentationDossier;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument.SectionPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogDossierEventPublisherTest {

    @Mock AuditEventService auditEvents;

    AuditLogDossierEventPublisher publisher;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final UUID FINALIZER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-06-22T09:00:00Z");

    @BeforeEach
    void setup() {
        publisher = new AuditLogDossierEventPublisher(auditEvents);
    }

    private DocumentationDossier dossier() {
        DossierDocument m = DossierDocument.planned("m", NormDocKind.MANUAL, "Manuel",
                List.of(new SectionPlan("s", "Section", List.of("4.1"), "")));
        m.markGenerated(UUID.randomUUID());
        DocumentationDossier d = DocumentationDossier.start(TENANT, UUID.randomUUID(), "iso-9001",
                "ISO 9001", "ACME", "fr", List.of(m), AUTHOR, NOW);
        d.assignId(UUID.randomUUID());
        return d;
    }

    @Test
    void publish_started_recordsAuditEvent_noPii() {
        DocumentationDossier d = dossier();
        publisher.publish(d, DossierEventPublisher.Action.STARTED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> captor =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), captor.capture());
        AuditEventDto.RecordEventRequest req = captor.getValue();
        assertThat(req.action()).isEqualTo("standards.dossier.started");
        assertThat(req.resourceType()).isEqualTo("standard-doc-dossier");
        assertThat(req.actorUserId()).isEqualTo(AUTHOR);
        assertThat(req.payloadJson()).contains("\"standardCode\":\"iso-9001\"");
        assertThat(req.payloadJson()).doesNotContain("ACME");
    }

    @Test
    void publish_finalized_usesFinalizerAsActor() {
        DocumentationDossier d = dossier();
        d.finalize("a".repeat(64), "sig", "tx", FINALIZER, NOW);
        publisher.publish(d, DossierEventPublisher.Action.FINALIZED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> captor =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), captor.capture());
        assertThat(captor.getValue().actorUserId()).isEqualTo(FINALIZER);
        assertThat(captor.getValue().action()).isEqualTo("standards.dossier.finalized");
    }

    @Test
    void publish_generated_usesCreator() {
        DocumentationDossier d = dossier();
        publisher.publish(d, DossierEventPublisher.Action.GENERATED);
        ArgumentCaptor<AuditEventDto.RecordEventRequest> captor =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), captor.capture());
        assertThat(captor.getValue().actorUserId()).isEqualTo(AUTHOR);
        assertThat(captor.getValue().action()).isEqualTo("standards.dossier.generated");
    }
}
