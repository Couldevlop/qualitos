package com.openlab.qualitos.quality.crossbordertransfers.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.crossbordertransfers.application.CrossBorderTransferEventPublisher;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransfer;
import com.openlab.qualitos.quality.crossbordertransfers.domain.TransferMechanism;
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
class AuditLogCrossBorderTransferEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_created_payloadHasMechanismAndCountries() {
        AuditLogCrossBorderTransferEventPublisher pub =
                new AuditLogCrossBorderTransferEventPublisher(auditEvents);
        CrossBorderTransfer t = sample();
        pub.publish(t, CrossBorderTransferEventPublisher.Action.CREATED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("gdpr.transfer.created");
        assertThat(req.resourceType()).isEqualTo("gdpr-cross-border-transfer");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.payloadJson())
                .contains("\"mechanism\":\"STANDARD_CONTRACTUAL_CLAUSES\"")
                .contains("\"countries\":1");
    }

    @Test
    void publish_allActions_prefixedGdprTransfer() {
        AuditLogCrossBorderTransferEventPublisher pub =
                new AuditLogCrossBorderTransferEventPublisher(auditEvents);
        CrossBorderTransfer t = sample();
        for (CrossBorderTransferEventPublisher.Action act
                : CrossBorderTransferEventPublisher.Action.values()) {
            pub.publish(t, act);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents,
                org.mockito.Mockito.times(CrossBorderTransferEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(req -> req.action().startsWith("gdpr.transfer."))
                .allMatch(req -> req.action().matches("gdpr\\.transfer\\.[a-z_]+"));
    }

    private CrossBorderTransfer sample() {
        CrossBorderTransfer t = CrossBorderTransfer.draft(T, "CBT-2026-001",
                "Acme Cloud Inc", null, "ops@acme.com",
                Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC 2021", null, null,
                Set.of(), Set.of(), Set.of(), U, NOW);
        t.assignId(ID);
        return t;
    }
}
