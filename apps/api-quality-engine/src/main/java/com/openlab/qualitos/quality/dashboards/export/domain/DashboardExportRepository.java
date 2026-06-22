package com.openlab.qualitos.quality.dashboards.export.domain;

import java.util.Optional;

/**
 * Port — persistence of signed dashboard export receipts. The domain depends on
 * this abstraction; the JPA adapter lives in infrastructure (hexagonal arch).
 */
public interface DashboardExportRepository {

    DashboardExport save(DashboardExport export);

    /**
     * Look up an export by its public verification code. No tenant filter here:
     * the opaque random code IS the authority (same pattern as Academy
     * certificate verification). Callers MUST NOT leak tenant-scoped data from
     * the result beyond the strict integrity facts.
     */
    Optional<DashboardExport> findByVerificationCode(String code);
}
