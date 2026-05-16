package com.openlab.qualitos.quality.blockchain.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AnchoringDto {

    private AnchoringDto() {}

    public record AnchorBatchRequest(UUID tenantId, int maxBatchSize) {}

    public record AnchorBatchResult(
            UUID tenantId,
            int batchSize,
            String merkleRoot,
            String blockchainTxRef,
            List<UUID> eventIds,
            long firstSequenceNo,
            long lastSequenceNo,
            Instant anchoredAt
    ) {
        public boolean isEmpty() { return batchSize == 0; }
    }
}
