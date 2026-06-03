package com.openlab.qualitos.quality.activityfeed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditActivityFeedConsumerTest {

    private final AuditActivityRepository repo = mock(AuditActivityRepository.class);
    private final ObjectMapper json = new ObjectMapper();
    private final AuditActivityFeedConsumer consumer = new AuditActivityFeedConsumer(repo, json);

    private final UUID tenant = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private String envelope(long seq) {
        return "{"
                + "\"id\":\"" + UUID.randomUUID() + "\","
                + "\"tenantId\":\"" + tenant + "\","
                + "\"sequenceNo\":" + seq + ","
                + "\"occurredAt\":\"2026-06-03T10:00:00Z\","
                + "\"recordedAt\":\"2026-06-03T10:00:01Z\","
                + "\"actorType\":\"USER\","
                + "\"actorUserId\":\"" + tenant + "\","
                + "\"action\":\"capa.created\","
                + "\"resourceType\":\"capa\","
                + "\"resourceId\":\"" + UUID.randomUUID() + "\","
                + "\"summary\":\"CAPA E2E\","
                + "\"payloadJson\":\"{\\\"k\\\":\\\"v\\\"}\","
                + "\"integrityHash\":\"abc\""
                + "}";
    }

    @Test
    void projectsNewEvent() {
        when(repo.existsByTenantIdAndSequenceNo(tenant, 7L)).thenReturn(false);

        consumer.consume(envelope(7L));

        ArgumentCaptor<AuditActivityEntry> cap = ArgumentCaptor.forClass(AuditActivityEntry.class);
        verify(repo).save(cap.capture());
        AuditActivityEntry e = cap.getValue();
        assertThat(e.getTenantId()).isEqualTo(tenant);
        assertThat(e.getSequenceNo()).isEqualTo(7L);
        assertThat(e.getAction()).isEqualTo("capa.created");
        assertThat(e.getResourceType()).isEqualTo("capa");
        assertThat(e.getSummary()).isEqualTo("CAPA E2E");
    }

    @Test
    void idempotent_skipsAlreadyProjected() {
        when(repo.existsByTenantIdAndSequenceNo(tenant, 7L)).thenReturn(true);

        consumer.consume(envelope(7L));

        verify(repo, never()).save(any());
    }

    @Test
    void malformedJson_isIgnoredWithoutThrowing() {
        consumer.consume("{ not json ");
        verify(repo, never()).existsByTenantIdAndSequenceNo(any(), eq(0L));
        verify(repo, never()).save(any());
    }

    @Test
    void incompleteEnvelope_isIgnored() {
        consumer.consume("{\"action\":\"capa.created\"}"); // pas de tenantId / sequenceNo
        verify(repo, never()).save(any());
    }
}
