package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.quality.blockchain.application.AnchoringDto;
import com.openlab.qualitos.quality.blockchain.application.AnchoringService;
import com.openlab.qualitos.quality.blockchain.domain.AnchorReadPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnchoringSchedulerTest {

    @Mock AnchorReadPort read;
    @Mock AnchoringService anchoring;

    static final UUID T1 = UUID.randomUUID();
    static final UUID T2 = UUID.randomUUID();

    AnchoringScheduler scheduler;

    @BeforeEach
    void setup() {
        scheduler = new AnchoringScheduler(read, anchoring);
    }

    private static AnchoringDto.AnchorBatchResult result(UUID tenant, int n) {
        return new AnchoringDto.AnchorBatchResult(
                tenant, n, "root", "tx", List.of(), 1L, n, Instant.now());
    }

    @Test
    void anchorsEachPendingTenant() {
        when(read.tenantsWithUnanchoredEvents()).thenReturn(List.of(T1, T2));
        when(anchoring.anchorBatch(any())).thenReturn(result(T1, 2));

        scheduler.anchorAllPendingTenants();

        verify(anchoring).anchorBatch(argThat(r -> r.tenantId().equals(T1)));
        verify(anchoring).anchorBatch(argThat(r -> r.tenantId().equals(T2)));
    }

    @Test
    void oneTenantFailureDoesNotBlockOthers() {
        when(read.tenantsWithUnanchoredEvents()).thenReturn(List.of(T1, T2));
        when(anchoring.anchorBatch(any())).thenAnswer(inv -> {
            AnchoringDto.AnchorBatchRequest req = inv.getArgument(0);
            if (req.tenantId().equals(T1)) {
                throw new IllegalStateException("boom");
            }
            return result(T2, 1);
        });

        scheduler.anchorAllPendingTenants();

        verify(anchoring, times(2)).anchorBatch(any());
    }

    @Test
    void noTenants_noAnchoring() {
        when(read.tenantsWithUnanchoredEvents()).thenReturn(List.of());

        scheduler.anchorAllPendingTenants();

        verify(anchoring, never()).anchorBatch(any());
    }
}
