package com.openlab.qualitos.quality.dashboards.export.application;

import java.util.UUID;

/**
 * Port — records the "official signed export" as an auditable event
 * (CLAUDE.md §18.2 #5: a signed/anchored artifact is an auditable action).
 * The infrastructure adapter forwards to the append-only audit-event chain.
 */
public interface DashboardExportAuditPort {

    /**
     * @param tenantId     tenant (from JWT)
     * @param actorUserId  acting user (JWT sub)
     * @param dashboardId  exported dashboard
     * @param sha256Hex    fingerprint of the rendered PDF
     * @param anchorTxRef  blockchain anchor reference
     */
    void recordExport(UUID tenantId, UUID actorUserId, UUID dashboardId,
                      String sha256Hex, String anchorTxRef);
}
