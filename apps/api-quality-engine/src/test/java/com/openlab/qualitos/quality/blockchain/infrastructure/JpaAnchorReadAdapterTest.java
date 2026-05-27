package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.quality.auditlog.AuditEvent;
import com.openlab.qualitos.quality.auditlog.AuditEventRepository;
import com.openlab.qualitos.quality.blockchain.domain.AnchorReadPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JpaAnchorReadAdapterTest {

    @Mock AuditEventRepository audit;
    @Mock AnchorReceiptRepository receipts;

    static final UUID TENANT = UUID.randomUUID();

    JpaAnchorReadAdapter adapter;

    @BeforeEach
    void setup() {
        adapter = new JpaAnchorReadAdapter(audit, receipts);
    }

    private AuditEvent event(String hash, String txRef) {
        AuditEvent e = new AuditEvent();
        e.setIntegrityHash(hash);
        e.setBlockchainTxRef(txRef);
        return e;
    }

    @Test
    void txRefForEvent_presentWhenAnchored() {
        when(audit.findByTenantIdAndIntegrityHash(TENANT, "h1"))
                .thenReturn(Optional.of(event("h1", "tx-1")));
        assertThat(adapter.txRefForEvent(TENANT, "h1")).contains("tx-1");
    }

    @Test
    void txRefForEvent_emptyWhenNotAnchored() {
        when(audit.findByTenantIdAndIntegrityHash(TENANT, "h1"))
                .thenReturn(Optional.of(event("h1", null)));
        assertThat(adapter.txRefForEvent(TENANT, "h1")).isEmpty();
    }

    @Test
    void txRefForEvent_emptyWhenEventAbsent() {
        when(audit.findByTenantIdAndIntegrityHash(TENANT, "h1")).thenReturn(Optional.empty());
        assertThat(adapter.txRefForEvent(TENANT, "h1")).isEmpty();
    }

    @Test
    void integrityHashesForTxRef_mapsInOrder() {
        when(audit.findByTenantIdAndBlockchainTxRefOrderBySequenceNoAsc(TENANT, "tx-1"))
                .thenReturn(List.of(event("h1", "tx-1"), event("h2", "tx-1")));
        assertThat(adapter.integrityHashesForTxRef(TENANT, "tx-1")).containsExactly("h1", "h2");
    }

    @Test
    void receipt_presentForValidUuid() {
        UUID id = UUID.randomUUID();
        AnchorReceiptEntity e = new AnchorReceiptEntity(
                id, TENANT, 1L, "a".repeat(64), "0".repeat(64), "c".repeat(64),
                "sig-encoded", Instant.now(), Instant.now());
        when(receipts.findByTenantIdAndId(TENANT, id)).thenReturn(Optional.of(e));

        Optional<AnchorReadPort.ReceiptView> view = adapter.receipt(TENANT, id.toString());

        assertThat(view).isPresent();
        assertThat(view.get().merkleRoot()).isEqualTo("a".repeat(64));
        assertThat(view.get().signature()).isEqualTo("sig-encoded");
    }

    @Test
    void receipt_emptyForNonUuidTxRef() {
        assertThat(adapter.receipt(TENANT, "stub-tx-123")).isEmpty();
    }

    @Test
    void receipt_emptyWhenAbsent() {
        UUID id = UUID.randomUUID();
        when(receipts.findByTenantIdAndId(TENANT, id)).thenReturn(Optional.empty());
        assertThat(adapter.receipt(TENANT, id.toString())).isEmpty();
    }

    @Test
    void tenantsWithUnanchored_delegates() {
        lenient().when(audit.findDistinctTenantIdsWithUnanchoredEvents()).thenReturn(List.of(TENANT));
        assertThat(adapter.tenantsWithUnanchoredEvents()).containsExactly(TENANT);
    }
}
