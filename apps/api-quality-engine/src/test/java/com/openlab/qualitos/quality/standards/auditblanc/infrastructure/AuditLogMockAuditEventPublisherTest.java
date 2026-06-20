package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/** Journalisation de l'audit blanc (§8.4 onglet 7, OWASP A09). */
@ExtendWith(MockitoExtension.class)
class AuditLogMockAuditEventPublisherTest {

    @Mock AuditEventService auditEvents;
    @Captor ArgumentCaptor<AuditEventDto.RecordEventRequest> reqCaptor;

    @Test
    void published_recordsMetadataForTenant_noPii() {
        UUID tenant = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        MockAuditRun run = MockAuditRun.of(tenant, UUID.randomUUID(), UUID.randomUUID(),
                "iso-9001", "ISO 9001:2015", 42d, List.of(), List.of(), List.of(),
                "ollama", actor, Instant.now());
        run.assignId(UUID.randomUUID());

        new AuditLogMockAuditEventPublisher(auditEvents).published(run);

        verify(auditEvents).recordForTenant(eq(tenant), reqCaptor.capture());
        AuditEventDto.RecordEventRequest req = reqCaptor.getValue();
        assertThat(req.action()).isEqualTo("standards.audit-blanc.executed");
        assertThat(req.resourceType()).isEqualTo("standard-mock-audit");
        assertThat(req.actorUserId()).isEqualTo(actor);
        assertThat(req.summary()).contains("iso-9001");
        assertThat(req.payloadJson()).contains("\"readiness\":42.0").contains("\"standardCode\":\"iso-9001\"");
    }
}
