package com.openlab.qualitos.quality.dashboards.export.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardExportAuditPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Records the signed-export action in the append-only, hash-chained audit log
 * (§11.5 / §18.2 #5). No PII in the payload — only the fingerprint and anchor ref.
 */
@Component
public class DashboardExportAuditAdapter implements DashboardExportAuditPort {

    private final AuditEventService auditEvents;

    public DashboardExportAuditAdapter(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void recordExport(UUID tenantId, UUID actorUserId, UUID dashboardId,
                             String sha256Hex, String anchorTxRef) {
        String payload = "{\"sha256\":\"" + sha256Hex + "\",\"anchorTxRef\":\""
                + anchorTxRef.replace("\"", "") + "\"}";
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                ActorType.USER,
                actorUserId,
                "dashboard.export.signed",
                "dashboard-export",
                dashboardId,
                "Export PDF signe + ancre d'un tableau de bord",
                payload,
                null,
                null));
    }
}
