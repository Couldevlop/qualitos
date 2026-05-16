package com.openlab.qualitos.quality.blockchain.domain;

import java.util.UUID;

/**
 * Port côté chaîne : soumet un Merkle root et renvoie la référence de
 * transaction. Sera implémenté par un adapter Hyperledger Fabric en prod
 * (chaincode Go cf. §11.3). En attendant, un adapter stub renvoie une
 * référence type "stub-tx-{uuid}".
 */
public interface BlockchainAnchorPort {

    /**
     * @return référence de transaction blockchain (string opaque, ≤ 200 chars).
     */
    String submitRoot(UUID tenantId, String merkleRootHex);
}
