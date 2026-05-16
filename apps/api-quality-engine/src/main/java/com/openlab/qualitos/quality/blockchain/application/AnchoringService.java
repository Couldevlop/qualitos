package com.openlab.qualitos.quality.blockchain.application;

import com.openlab.qualitos.quality.blockchain.domain.Anchorable;
import com.openlab.qualitos.quality.blockchain.domain.AnchorablesPort;
import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.blockchain.domain.MerkleTree;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use case : prendre les N événements d'audit non ancrés d'un tenant,
 * calculer leur racine Merkle, la soumettre à la blockchain, marquer les
 * événements avec la txRef retournée.
 *
 * Le tenant est passé explicitement (appel typique : scheduler) — le port
 * tenantProvider n'est pas requis ici.
 */
public class AnchoringService {

    static final int DEFAULT_BATCH = 100;
    static final int MAX_BATCH = 1000;

    private final AnchorablesPort anchorables;
    private final BlockchainAnchorPort blockchain;
    private final Clock clock;

    public AnchoringService(AnchorablesPort anchorables,
                            BlockchainAnchorPort blockchain,
                            Clock clock) {
        this.anchorables = anchorables;
        this.blockchain = blockchain;
        this.clock = clock;
    }

    public AnchoringDto.AnchorBatchResult anchorBatch(AnchoringDto.AnchorBatchRequest req) {
        if (req.tenantId() == null) {
            throw new IllegalArgumentException("tenantId required");
        }
        int limit = req.maxBatchSize() <= 0 ? DEFAULT_BATCH
                : Math.min(req.maxBatchSize(), MAX_BATCH);
        List<Anchorable> events = anchorables.loadUnanchored(req.tenantId(), limit);
        Instant ranAt = Instant.now(clock);
        if (events.isEmpty()) {
            return new AnchoringDto.AnchorBatchResult(
                    req.tenantId(), 0, null, null, List.of(), 0L, 0L, ranAt);
        }
        String root = MerkleTree.root(events.stream().map(Anchorable::integrityHash).toList());
        String txRef = blockchain.submitRoot(req.tenantId(), root);
        if (txRef == null || txRef.isBlank()) {
            throw new IllegalStateException("Blockchain adapter returned blank tx ref");
        }
        List<UUID> ids = events.stream().map(Anchorable::id).toList();
        anchorables.markAnchored(req.tenantId(), ids, txRef);
        return new AnchoringDto.AnchorBatchResult(
                req.tenantId(), events.size(), root, txRef, ids,
                events.get(0).sequenceNo(),
                events.get(events.size() - 1).sequenceNo(),
                ranAt);
    }
}
