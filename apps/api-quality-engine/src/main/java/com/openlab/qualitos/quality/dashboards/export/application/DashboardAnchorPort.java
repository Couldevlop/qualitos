package com.openlab.qualitos.quality.dashboards.export.application;

import java.util.UUID;

/**
 * Port — anchors an export fingerprint and returns a transaction reference.
 * Bridged in infrastructure to the platform's blockchain anchor port (stub in
 * dev, Hyperledger Fabric in prod). Declared here so the export application
 * stays free of the blockchain module's packages.
 */
public interface DashboardAnchorPort {

    /** @return blockchain transaction reference (opaque string). */
    String submitRoot(UUID tenantId, String sha256Hex);
}
