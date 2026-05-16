package com.openlab.qualitos.quality.privacynotices.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.privacynotices.application.PrivacyNoticeEventPublisher;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNotice;
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
class AuditLogPrivacyNoticeEventPublisherTest {

    @Mock AuditEventService auditEvents;

    static final UUID T = UUID.randomUUID();
    static final UUID CREATOR = UUID.randomUUID();
    static final UUID PUB = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void publish_created_actorIsCreator() {
        AuditLogPrivacyNoticeEventPublisher pub =
                new AuditLogPrivacyNoticeEventPublisher(auditEvents);
        PrivacyNotice n = sample();
        pub.publish(n, PrivacyNoticeEventPublisher.Action.CREATED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        AuditEventDto.RecordEventRequest req = cap.getValue();
        assertThat(req.action()).isEqualTo("gdpr.notice.created");
        assertThat(req.resourceType()).isEqualTo("gdpr-privacy-notice");
        assertThat(req.actorType()).isEqualTo(ActorType.USER);
        assertThat(req.actorUserId()).isEqualTo(CREATOR);
        assertThat(req.payloadJson()).contains("\"language\":\"fr\"");
    }

    @Test
    void publish_published_actorIsPublishedBy() {
        AuditLogPrivacyNoticeEventPublisher pub =
                new AuditLogPrivacyNoticeEventPublisher(auditEvents);
        PrivacyNotice n = sample();
        n.publish(PUB, NOW);

        pub.publish(n, PrivacyNoticeEventPublisher.Action.PUBLISHED);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(T), cap.capture());
        assertThat(cap.getValue().actorUserId()).isEqualTo(PUB);
    }

    @Test
    void publish_allActions_prefixedGdprNotice() {
        AuditLogPrivacyNoticeEventPublisher pub =
                new AuditLogPrivacyNoticeEventPublisher(auditEvents);
        PrivacyNotice n = sample();
        for (PrivacyNoticeEventPublisher.Action a : PrivacyNoticeEventPublisher.Action.values()) {
            pub.publish(n, a);
        }
        ArgumentCaptor<AuditEventDto.RecordEventRequest> cap =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents,
                org.mockito.Mockito.times(PrivacyNoticeEventPublisher.Action.values().length))
                .recordForTenant(eq(T), cap.capture());
        assertThat(cap.getAllValues())
                .allMatch(r -> r.action().startsWith("gdpr.notice."))
                .allMatch(r -> r.action().matches("gdpr\\.notice\\.[a-z_]+"));
    }

    private PrivacyNotice sample() {
        PrivacyNotice n = PrivacyNotice.draft(T, "PN-CUSTOMERS", "1.0", "fr",
                "Mention", "résumé valide", "contenu",
                Set.of(), null, null, null, CREATOR, NOW);
        n.assignId(ID);
        return n;
    }
}
