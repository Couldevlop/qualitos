package com.openlab.qualitos.quality.blockchain.infrastructure;

import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ancrage Phase B (ADR 0012) : soumet le Merkle root au réseau Hyperledger Fabric
 * via {@link FabricGatewayClient} → {@code blockchain-service} → chaincode
 * {@code qualitos-anchor}. Renvoie la référence de transaction Fabric.
 *
 * <p><b>Repli</b> : si Fabric est indisponible, on retombe sur la notarisation
 * signée Phase A ({@link SignedAnchorAdapter}) — l'ancrage n'est jamais perdu
 * (ADR 0012 : « SignedAnchorAdapter reste le fallback si Fabric est indisponible »).
 *
 * <p>Actif sous le profil {@code fabric} et {@link Primary} : il devient alors le
 * {@link BlockchainAnchorPort} injecté ; sans ce profil, {@code SignedAnchorAdapter}
 * (défaut) reste seul. RGPD §11.3 : seul un hash (Merkle root) part on-chain.
 */
@Component
@Profile("fabric")
@Primary
public class FabricBlockchainAnchorAdapter implements BlockchainAnchorPort {

    private static final Logger log = LoggerFactory.getLogger(FabricBlockchainAnchorAdapter.class);

    /** Préfixe distinguant un txRef Fabric d'un id de reçu signé (Phase A). */
    static final String FABRIC_TX_PREFIX = "fabric:";

    private final FabricGatewayClient fabric;
    private final SignedAnchorAdapter signedFallback;

    public FabricBlockchainAnchorAdapter(FabricGatewayClient fabric,
                                         SignedAnchorAdapter signedFallback) {
        this.fabric = fabric;
        this.signedFallback = signedFallback;
    }

    @Override
    public String submitRoot(UUID tenantId, String merkleRootHex) {
        try {
            String txId = fabric.anchor(tenantId, merkleRootHex);
            log.info("[blockchain-fabric] anchored tenant={} root={} → fabricTx={}",
                    tenantId, merkleRootHex, txId);
            return FABRIC_TX_PREFIX + txId;
        } catch (FabricAnchorException e) {
            log.warn("[blockchain-fabric] Fabric indisponible, repli sur reçu signé (Phase A) : {}",
                    e.getMessage());
            return signedFallback.submitRoot(tenantId, merkleRootHex);
        }
    }
}
