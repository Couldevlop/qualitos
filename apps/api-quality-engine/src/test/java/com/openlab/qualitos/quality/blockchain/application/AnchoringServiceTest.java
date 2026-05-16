package com.openlab.qualitos.quality.blockchain.application;

import com.openlab.qualitos.quality.blockchain.domain.Anchorable;
import com.openlab.qualitos.quality.blockchain.domain.AnchorablesPort;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.blockchain.domain.MerkleTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnchoringServiceTest {

    @Mock AnchorablesPort anchorables;
    @Mock BlockchainAnchorPort blockchain;
    AnchoringService service;

    static final UUID TENANT = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @BeforeEach
    void setup() {
        service = new AnchoringService(anchorables, blockchain,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void empty_batch_returnsEmptyResult_andNoBlockchainCall() {
        when(anchorables.loadUnanchored(eq(TENANT), anyInt())).thenReturn(List.of());
        AnchoringDto.AnchorBatchResult r = service.anchorBatch(
                new AnchoringDto.AnchorBatchRequest(TENANT, 100));
        assertThat(r.isEmpty()).isTrue();
        assertThat(r.batchSize()).isZero();
        assertThat(r.merkleRoot()).isNull();
        verifyNoInteractions(blockchain);
        verify(anchorables, never()).markAnchored(any(), any(), any());
    }

    @Test
    void nonEmpty_batch_computesRoot_submits_andMarks() {
        List<Anchorable> events = List.of(
                new Anchorable(UUID.randomUUID(), 1L, "aaa"),
                new Anchorable(UUID.randomUUID(), 2L, "bbb"),
                new Anchorable(UUID.randomUUID(), 3L, "ccc"));
        when(anchorables.loadUnanchored(eq(TENANT), anyInt())).thenReturn(events);
        when(blockchain.submitRoot(eq(TENANT), any())).thenReturn("tx-xyz");
        when(anchorables.markAnchored(eq(TENANT), any(), eq("tx-xyz"))).thenReturn(3);

        AnchoringDto.AnchorBatchResult r = service.anchorBatch(
                new AnchoringDto.AnchorBatchRequest(TENANT, 100));

        String expectedRoot = MerkleTree.root(List.of("aaa", "bbb", "ccc"));
        assertThat(r.merkleRoot()).isEqualTo(expectedRoot);
        assertThat(r.blockchainTxRef()).isEqualTo("tx-xyz");
        assertThat(r.batchSize()).isEqualTo(3);
        assertThat(r.firstSequenceNo()).isEqualTo(1L);
        assertThat(r.lastSequenceNo()).isEqualTo(3L);
        assertThat(r.anchoredAt()).isEqualTo(NOW);
    }

    @Test
    void batchSizeZero_fallsBackToDefault() {
        when(anchorables.loadUnanchored(eq(TENANT), eq(AnchoringService.DEFAULT_BATCH)))
                .thenReturn(List.of());
        service.anchorBatch(new AnchoringDto.AnchorBatchRequest(TENANT, 0));
        verify(anchorables).loadUnanchored(TENANT, AnchoringService.DEFAULT_BATCH);
    }

    @Test
    void batchSize_clampedToMax() {
        when(anchorables.loadUnanchored(eq(TENANT), eq(AnchoringService.MAX_BATCH)))
                .thenReturn(List.of());
        service.anchorBatch(new AnchoringDto.AnchorBatchRequest(TENANT, 99_999));
        verify(anchorables).loadUnanchored(TENANT, AnchoringService.MAX_BATCH);
    }

    @Test
    void nullTenant_rejected() {
        assertThatThrownBy(() -> service.anchorBatch(
                new AnchoringDto.AnchorBatchRequest(null, 100)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankTxRef_rejected() {
        when(anchorables.loadUnanchored(eq(TENANT), anyInt()))
                .thenReturn(List.of(new Anchorable(UUID.randomUUID(), 1L, "h")));
        when(blockchain.submitRoot(eq(TENANT), any())).thenReturn("   ");
        assertThatThrownBy(() -> service.anchorBatch(
                new AnchoringDto.AnchorBatchRequest(TENANT, 100)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blank");
        // Garde-fou : pas de marquage si tx ref vide.
        verify(anchorables, never()).markAnchored(any(), any(), any());
    }

    @Test
    void markAnchored_receivesEventIdsInOrder() {
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        when(anchorables.loadUnanchored(eq(TENANT), anyInt())).thenReturn(List.of(
                new Anchorable(e1, 5L, "h1"),
                new Anchorable(e2, 6L, "h2")));
        when(blockchain.submitRoot(eq(TENANT), any())).thenReturn("tx");
        service.anchorBatch(new AnchoringDto.AnchorBatchRequest(TENANT, 100));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> ids = ArgumentCaptor.forClass(List.class);
        verify(anchorables).markAnchored(eq(TENANT), ids.capture(), eq("tx"));
        assertThat(ids.getValue()).containsExactly(e1, e2);
    }
}
