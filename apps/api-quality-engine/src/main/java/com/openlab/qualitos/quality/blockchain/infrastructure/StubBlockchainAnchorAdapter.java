package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter de TEST uniquement (profil "test") : renvoie une référence opaque.
 * En run réel, {@link SignedAnchorAdapter} (profil "!test") signe le Merkle root
 * et persiste un reçu chaîné vérifiable (ADR 0012 Phase A).
 */
@Component
@Profile("test")
public class StubBlockchainAnchorAdapter implements BlockchainAnchorPort {

    private static final Logger log = LoggerFactory.getLogger(StubBlockchainAnchorAdapter.class);

    @Override
    public String submitRoot(UUID tenantId, String merkleRootHex) {
        String txRef = "stub-tx-" + UUID.randomUUID();
        log.info("[blockchain-stub] anchored tenant={} root={} → tx={}",
                tenantId, merkleRootHex, txRef);
        return txRef;
    }
}
