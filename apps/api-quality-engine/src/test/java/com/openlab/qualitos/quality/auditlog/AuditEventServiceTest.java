package com.openlab.qualitos.quality.auditlog;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    @Mock AuditEventRepository eventRepo;
    @Mock AuditEventCounterRepository counterRepo;
    AuditEventService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID EVT = UUID.randomUUID();
    static final UUID RES = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-15T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new AuditEventService(eventRepo, counterRepo, CLOCK);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    // ---- Recording ----

    @Test
    void record_firstEvent_sequenceStartsAt1_noPreviousHash() {
        when(counterRepo.findById(TENANT)).thenReturn(Optional.empty());
        when(counterRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepo.findTopByTenantIdOrderBySequenceNoDesc(TENANT)).thenReturn(Optional.empty());
        when(eventRepo.save(any())).thenAnswer(inv -> {
            AuditEvent e = inv.getArgument(0);
            e.setId(EVT); return e;
        });
        AuditEventDto.EventResponse out = service.record(new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, USER, "pdca.cycle.created", "pdca-cycle",
                RES, "Cycle X created", "{\"x\":1}", "127.0.0.1", "agent"));
        assertThat(out.sequenceNo()).isEqualTo(1L);
        assertThat(out.previousHash()).isNull();
        assertThat(out.integrityHash()).matches("^[0-9a-f]{64}$");
        assertThat(out.recordedAt()).isEqualTo(NOW);
        assertThat(out.occurredAt()).isEqualTo(NOW); // défaut clock
    }

    @Test
    void record_chainsToLatestPreviousHash() {
        AuditEventCounter ctr = new AuditEventCounter(TENANT, 5L);
        when(counterRepo.findById(TENANT)).thenReturn(Optional.of(ctr));
        when(counterRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AuditEvent prev = new AuditEvent();
        prev.setId(UUID.randomUUID()); prev.setTenantId(TENANT);
        prev.setSequenceNo(5L);
        prev.setIntegrityHash("a".repeat(64));
        when(eventRepo.findTopByTenantIdOrderBySequenceNoDesc(TENANT)).thenReturn(Optional.of(prev));
        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        when(eventRepo.save(cap.capture())).thenAnswer(inv -> {
            AuditEvent e = inv.getArgument(0);
            e.setId(EVT); return e;
        });

        service.record(new AuditEventDto.RecordEventRequest(
                Instant.parse("2026-05-14T09:00:00Z"), ActorType.SYSTEM, null,
                "capa.opened", "capa", RES, "CAPA C-1 opened", null, null, null));

        AuditEvent saved = cap.getValue();
        assertThat(saved.getSequenceNo()).isEqualTo(6L);
        assertThat(saved.getPreviousHash()).isEqualTo("a".repeat(64));
        assertThat(saved.getOccurredAt()).isEqualTo(Instant.parse("2026-05-14T09:00:00Z"));
        // counter avancé
        assertThat(ctr.getLastSequenceNo()).isEqualTo(6L);
    }

    @Test
    void record_noTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.record(new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, USER, "x.y", "foo", null, null, null, null, null)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void recordForTenant_explicitTenant_bypassesContext() {
        TenantContext.clear();
        UUID other = UUID.randomUUID();
        when(counterRepo.findById(other)).thenReturn(Optional.empty());
        when(counterRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepo.findTopByTenantIdOrderBySequenceNoDesc(other)).thenReturn(Optional.empty());
        when(eventRepo.save(any())).thenAnswer(inv -> {
            AuditEvent e = inv.getArgument(0);
            e.setId(EVT); return e;
        });
        AuditEvent out = service.recordForTenant(other, new AuditEventDto.RecordEventRequest(
                null, ActorType.SCHEDULER, null, "scheduler.tick", "scheduler",
                null, null, null, null, null));
        assertThat(out.getTenantId()).isEqualTo(other);
        assertThat(out.getSequenceNo()).isEqualTo(1L);
    }

    @Test
    void recordForTenant_nullTenant_throws() {
        assertThatThrownBy(() -> service.recordForTenant(null,
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.SYSTEM, null, "x.y", "foo",
                        null, null, null, null, null)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void record_integrityHashIsRecomputable() {
        when(counterRepo.findById(TENANT)).thenReturn(Optional.empty());
        when(counterRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepo.findTopByTenantIdOrderBySequenceNoDesc(TENANT)).thenReturn(Optional.empty());
        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        when(eventRepo.save(cap.capture())).thenAnswer(inv -> {
            AuditEvent e = inv.getArgument(0); e.setId(EVT); return e;
        });
        service.record(new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, USER, "pdca.x", "foo", RES, "s", null, null, null));
        AuditEvent saved = cap.getValue();
        assertThat(AuditEventHasher.hash(saved)).isEqualTo(saved.getIntegrityHash());
    }

    // ---- Listing ----

    @Test
    void list_filterByResource() {
        when(eventRepo.findByTenantIdAndResourceTypeAndResourceIdOrderBySequenceNoDesc(
                eq(TENANT), eq("capa"), eq(RES), any()))
                .thenReturn(new PageImpl<>(List.of(event(7L))));
        assertThat(service.list(null, "capa", RES, null, null, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    @Test
    void list_filterByAction() {
        when(eventRepo.findByTenantIdAndActionOrderBySequenceNoDesc(
                eq(TENANT), eq("pdca.cycle.created"), any()))
                .thenReturn(new PageImpl<>(List.of(event(1L))));
        assertThat(service.list("pdca.cycle.created", null, null, null, null, null,
                PageRequest.of(0, 10)).getTotalElements()).isOne();
    }

    @Test
    void list_filterByActor() {
        when(eventRepo.findByTenantIdAndActorUserIdOrderBySequenceNoDesc(eq(TENANT), eq(USER), any()))
                .thenReturn(new PageImpl<>(List.of(event(2L))));
        assertThat(service.list(null, null, null, USER, null, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    @Test
    void list_filterByPeriod() {
        Instant from = Instant.parse("2026-05-01T00:00:00Z");
        Instant to = Instant.parse("2026-05-31T23:59:59Z");
        when(eventRepo.findByTenantIdAndOccurredAtBetweenOrderBySequenceNoDesc(
                eq(TENANT), eq(from), eq(to), any()))
                .thenReturn(new PageImpl<>(List.of(event(3L))));
        assertThat(service.list(null, null, null, null, from, to, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    @Test
    void list_noFilter_allTenantEvents() {
        when(eventRepo.findByTenantIdOrderBySequenceNoDesc(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(event(1L))));
        assertThat(service.list(null, null, null, null, null, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    @Test
    void get_crossTenant_appearsNotFound() {
        AuditEvent e = event(1L);
        e.setTenantId(UUID.randomUUID());
        when(eventRepo.findById(EVT)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.get(EVT))
                .isInstanceOf(AuditEventNotFoundException.class);
    }

    // ---- Chain verification ----

    @Test
    void verifyChain_invertedRange_rejected() {
        assertThatThrownBy(() -> service.verifyChain(1L, 0L))
                .isInstanceOf(AuditEventStateException.class);
        verifyNoInteractions(eventRepo);
    }

    @Test
    void verifyChain_validChain_noBreaks() {
        // Construit 3 événements bien chaînés.
        AuditEvent e1 = event(1L);
        e1.setPreviousHash(null);
        e1.setIntegrityHash(AuditEventHasher.hash(e1));
        AuditEvent e2 = event(2L);
        e2.setPreviousHash(e1.getIntegrityHash());
        e2.setIntegrityHash(AuditEventHasher.hash(e2));
        AuditEvent e3 = event(3L);
        e3.setPreviousHash(e2.getIntegrityHash());
        e3.setIntegrityHash(AuditEventHasher.hash(e3));
        when(eventRepo.findByTenantIdAndSequenceNoBetweenOrderBySequenceNoAsc(TENANT, 1L, 3L))
                .thenReturn(List.of(e1, e2, e3));

        AuditEventDto.ChainVerification v = service.verifyChain(1L, 3L);
        assertThat(v.valid()).isTrue();
        assertThat(v.breaks()).isEmpty();
        assertThat(v.verifiedCount()).isEqualTo(3L);
    }

    @Test
    void verifyChain_tamperedHash_detected() {
        AuditEvent e1 = event(1L);
        e1.setPreviousHash(null);
        e1.setIntegrityHash(AuditEventHasher.hash(e1));
        AuditEvent e2 = event(2L);
        e2.setPreviousHash(e1.getIntegrityHash());
        e2.setIntegrityHash("ffff".repeat(16)); // tampered
        when(eventRepo.findByTenantIdAndSequenceNoBetweenOrderBySequenceNoAsc(TENANT, 1L, 2L))
                .thenReturn(List.of(e1, e2));
        AuditEventDto.ChainVerification v = service.verifyChain(1L, 2L);
        assertThat(v.valid()).isFalse();
        assertThat(v.breaks()).anyMatch(b -> b.reason().contains("Integrity hash mismatch"));
    }

    @Test
    void verifyChain_brokenChainLink_detected() {
        AuditEvent e1 = event(1L);
        e1.setPreviousHash(null);
        e1.setIntegrityHash(AuditEventHasher.hash(e1));
        AuditEvent e2 = event(2L);
        e2.setPreviousHash("00".repeat(32)); // mauvais previous hash
        e2.setIntegrityHash(AuditEventHasher.hash(e2));
        when(eventRepo.findByTenantIdAndSequenceNoBetweenOrderBySequenceNoAsc(TENANT, 1L, 2L))
                .thenReturn(List.of(e1, e2));
        AuditEventDto.ChainVerification v = service.verifyChain(1L, 2L);
        assertThat(v.valid()).isFalse();
        assertThat(v.breaks()).anyMatch(b -> b.reason().contains("Previous hash mismatch"));
    }

    @Test
    void verifyChain_sequenceGap_detected() {
        AuditEvent e1 = event(1L);
        e1.setPreviousHash(null);
        e1.setIntegrityHash(AuditEventHasher.hash(e1));
        // Saute seq 2.
        AuditEvent e3 = event(3L);
        e3.setPreviousHash(e1.getIntegrityHash());
        e3.setIntegrityHash(AuditEventHasher.hash(e3));
        when(eventRepo.findByTenantIdAndSequenceNoBetweenOrderBySequenceNoAsc(TENANT, 1L, 3L))
                .thenReturn(List.of(e1, e3));
        AuditEventDto.ChainVerification v = service.verifyChain(1L, 3L);
        assertThat(v.valid()).isFalse();
        assertThat(v.breaks()).anyMatch(b -> b.reason().startsWith("Sequence gap"));
    }

    @Test
    void verifyChain_missingTail_detected() {
        AuditEvent e1 = event(1L);
        e1.setPreviousHash(null);
        e1.setIntegrityHash(AuditEventHasher.hash(e1));
        when(eventRepo.findByTenantIdAndSequenceNoBetweenOrderBySequenceNoAsc(TENANT, 1L, 5L))
                .thenReturn(List.of(e1));
        AuditEventDto.ChainVerification v = service.verifyChain(1L, 5L);
        assertThat(v.valid()).isFalse();
        assertThat(v.breaks()).anyMatch(b -> b.reason().startsWith("Missing event"));
    }

    @Test
    void verifyChain_fromMiddle_usesPredecessorHash() {
        // Range [3, 3], predecessor seq=2 doit être pris comme expectedPrevious.
        AuditEvent e2 = event(2L);
        e2.setIntegrityHash("p".repeat(64));
        AuditEvent e3 = event(3L);
        e3.setPreviousHash("p".repeat(64));
        e3.setIntegrityHash(AuditEventHasher.hash(e3));
        when(eventRepo.findByTenantIdAndSequenceNoBetweenOrderBySequenceNoAsc(TENANT, 2L, 2L))
                .thenReturn(List.of(e2));
        when(eventRepo.findByTenantIdAndSequenceNoBetweenOrderBySequenceNoAsc(TENANT, 3L, 3L))
                .thenReturn(List.of(e3));
        AuditEventDto.ChainVerification v = service.verifyChain(3L, 3L);
        assertThat(v.valid()).isTrue();
    }

    // ---- Anchor ----

    @Test
    void anchor_setsTxRef() {
        AuditEvent e = event(1L);
        when(eventRepo.findById(EVT)).thenReturn(Optional.of(e));
        when(eventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AuditEventDto.EventResponse out = service.anchor(EVT,
                new AuditEventDto.AnchorRequest("fabric-tx-abc"));
        assertThat(out.blockchainTxRef()).isEqualTo("fabric-tx-abc");
    }

    @Test
    void anchor_alreadyAnchored_rejected() {
        AuditEvent e = event(1L);
        e.setBlockchainTxRef("existing");
        when(eventRepo.findById(EVT)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.anchor(EVT, new AuditEventDto.AnchorRequest("new")))
                .isInstanceOf(AuditEventStateException.class);
    }

    @Test
    void anchor_crossTenant_appearsNotFound() {
        AuditEvent e = event(1L);
        e.setTenantId(UUID.randomUUID());
        when(eventRepo.findById(EVT)).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.anchor(EVT, new AuditEventDto.AnchorRequest("x")))
                .isInstanceOf(AuditEventNotFoundException.class);
    }

    // ---- helpers ----

    private AuditEvent event(long seq) {
        AuditEvent e = new AuditEvent();
        e.setId(EVT);
        e.setTenantId(TENANT);
        e.setSequenceNo(seq);
        e.setOccurredAt(Instant.parse("2026-05-15T09:00:00Z"));
        e.setRecordedAt(Instant.parse("2026-05-15T09:00:01Z"));
        e.setActorType(ActorType.USER);
        e.setActorUserId(USER);
        e.setAction("pdca.cycle.created");
        e.setResourceType("pdca-cycle");
        e.setResourceId(RES);
        e.setSummary("s");
        e.setPayloadJson("{}");
        return e;
    }
}
