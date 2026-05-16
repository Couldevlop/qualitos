package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter par défaut, en attente du vrai client Hyperledger Fabric (§11.3).
 * Renvoie une référence opaque déterministe par tenant + root pour faciliter
 * les tests d'intégration. La prod doit fournir un bean concret qui désactive
 * celui-ci ({@link ConditionalOnMissingBean} préserverait l'override).
 */
@Component
@ConditionalOnMissingBean(BlockchainAnchorPort.class)
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
