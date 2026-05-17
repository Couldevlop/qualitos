package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter par défaut, en attente du vrai client Hyperledger Fabric (§11.3).
 * Renvoie une référence opaque déterministe par tenant + root pour faciliter
 * les tests d'intégration. Pour overrider en prod : declarer un autre bean
 * BlockchainAnchorPort + marquer ce stub @Profile("dev") ou supprimer son @Component.
 */
@Component
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
