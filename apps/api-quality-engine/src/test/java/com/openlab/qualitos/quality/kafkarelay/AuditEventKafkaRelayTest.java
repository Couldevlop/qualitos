package com.openlab.qualitos.quality.kafkarelay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEvent;
import com.openlab.qualitos.quality.auditlog.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class AuditEventKafkaRelayTest {

    private final AuditEventRepository events = mock(AuditEventRepository.class);
    private final KafkaRelayCursorRepository cursors = mock(KafkaRelayCursorRepository.class);
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafka =
            mock(org.springframework.kafka.core.KafkaTemplate.class);
    private final ObjectMapper json = new ObjectMapper();
    private final String topic = "qualitos.audit-events";

    private final AuditEventKafkaRelay relay =
            new AuditEventKafkaRelay(events, cursors, kafka, json, topic, 200);

    private final UUID tenant = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private AuditEvent event(long seq, String action) {
        AuditEvent e = new AuditEvent();
        e.setId(UUID.randomUUID());
        e.setTenantId(tenant);
        e.setSequenceNo(seq);
        e.setOccurredAt(Instant.parse("2026-06-03T10:00:00Z"));
        e.setRecordedAt(Instant.parse("2026-06-03T10:00:01Z"));
        e.setActorType(ActorType.USER);
        e.setAction(action);
        e.setResourceType("capa");
        e.setPayloadJson("{\"k\":\"v\"}");
        e.setIntegrityHash("abc123");
        return e;
    }

    @Test
    void publishesNewEvents_keyedByTenant_andAdvancesCursor() {
        when(cursors.findById(tenant)).thenReturn(Optional.empty());
        when(events.findByTenantIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
                eq(tenant), eq(0L), any(Pageable.class)))
                .thenReturn(List.of(event(1, "capa.created"), event(2, "capa.updated")));

        relay.relayTenant(tenant);

        verify(kafka, times(2)).send(eq(topic), eq(tenant.toString()), anyString());
        verify(kafka).flush();

        ArgumentCaptor<KafkaRelayCursor> saved = ArgumentCaptor.forClass(KafkaRelayCursor.class);
        verify(cursors).save(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo(tenant);
        assertThat(saved.getValue().getLastPublishedSeq()).isEqualTo(2L);
    }

    @Test
    void resumesFromExistingCursor() {
        when(cursors.findById(tenant))
                .thenReturn(Optional.of(new KafkaRelayCursor(tenant, 5L, Instant.now())));
        when(events.findByTenantIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
                eq(tenant), eq(5L), any(Pageable.class)))
                .thenReturn(List.of(event(6, "capa.closed")));

        relay.relayTenant(tenant);

        verify(kafka, times(1)).send(eq(topic), eq(tenant.toString()), anyString());
        ArgumentCaptor<KafkaRelayCursor> saved = ArgumentCaptor.forClass(KafkaRelayCursor.class);
        verify(cursors).save(saved.capture());
        assertThat(saved.getValue().getLastPublishedSeq()).isEqualTo(6L);
    }

    @Test
    void emptyBatch_publishesNothing_andDoesNotTouchCursor() {
        when(cursors.findById(tenant)).thenReturn(Optional.empty());
        when(events.findByTenantIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
                eq(tenant), eq(0L), any(Pageable.class)))
                .thenReturn(List.of());

        relay.relayTenant(tenant);

        verifyNoInteractions(kafka);
        verify(cursors, never()).save(any());
    }

    @Test
    void envelopeIsJsonWithKeyFields() throws Exception {
        when(cursors.findById(tenant)).thenReturn(Optional.empty());
        when(events.findByTenantIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
                eq(tenant), eq(0L), any(Pageable.class)))
                .thenReturn(List.of(event(1, "capa.created")));

        relay.relayTenant(tenant);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq(topic), eq(tenant.toString()), payload.capture());
        var node = json.readTree(payload.getValue());
        assertThat(node.get("tenantId").asText()).isEqualTo(tenant.toString());
        assertThat(node.get("sequenceNo").asLong()).isEqualTo(1L);
        assertThat(node.get("action").asText()).isEqualTo("capa.created");
        assertThat(node.get("resourceType").asText()).isEqualTo("capa");
        assertThat(node.get("integrityHash").asText()).isEqualTo("abc123");
    }

    @Test
    void relayPending_iteratesEachTenant() {
        when(events.findDistinctTenantIds()).thenReturn(List.of(tenant));
        when(cursors.findById(tenant)).thenReturn(Optional.empty());
        when(events.findByTenantIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
                eq(tenant), eq(0L), any(Pageable.class)))
                .thenReturn(List.of());

        relay.relayPending();

        verify(events).findDistinctTenantIds();
    }

    @Test
    void relayPending_oneTenantFailureDoesNotStopOthers() {
        UUID other = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        when(events.findDistinctTenantIds()).thenReturn(List.of(tenant, other));
        when(cursors.findById(tenant)).thenThrow(new RuntimeException("boom"));
        when(cursors.findById(other)).thenReturn(Optional.empty());
        when(events.findByTenantIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
                eq(other), eq(0L), any(Pageable.class)))
                .thenReturn(List.of());

        relay.relayPending();

        // le tenant 'other' est traité malgré l'échec du premier
        verify(cursors).findById(other);
    }
}
