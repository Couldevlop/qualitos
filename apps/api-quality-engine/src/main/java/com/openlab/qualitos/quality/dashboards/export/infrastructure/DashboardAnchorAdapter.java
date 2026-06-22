package com.openlab.qualitos.quality.dashboards.export.infrastructure;

import com.openlab.qualitos.quality.blockchain.domain.BlockchainAnchorPort;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardAnchorPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Bridges the export anchor port to the platform's blockchain anchor port
 * (stub in dev, Hyperledger Fabric in prod — §11.3). RGPD-safe: only the
 * SHA-256 fingerprint goes on-chain, never personal data.
 */
@Component
public class DashboardAnchorAdapter implements DashboardAnchorPort {

    private final BlockchainAnchorPort blockchain;

    public DashboardAnchorAdapter(BlockchainAnchorPort blockchain) {
        this.blockchain = blockchain;
    }

    @Override
    public String submitRoot(UUID tenantId, String sha256Hex) {
        return blockchain.submitRoot(tenantId, sha256Hex);
    }
}
