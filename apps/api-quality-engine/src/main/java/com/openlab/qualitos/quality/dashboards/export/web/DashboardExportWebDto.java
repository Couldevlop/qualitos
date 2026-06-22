package com.openlab.qualitos.quality.dashboards.export.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Web request payloads for the signed PDF export (§7.3 / §7.4). Note: there is
 * NO tenant/dashboard id in the body — the dashboard id is a path variable and
 * the tenant comes from the JWT (§18.2 #2).
 */
public final class DashboardExportWebDto {

    private DashboardExportWebDto() {}

    /**
     * Optional widget snapshots to embed (the on-screen data). Bounded to keep
     * the export deterministic and to prevent oversized payloads (OWASP A04/A10).
     */
    public record ExportRequest(
            @Size(max = 200) @Valid List<WidgetSnapshot> widgets) {}

    public record WidgetSnapshot(
            @Size(max = 200) String title,
            @Size(max = 40) String type,
            @Size(max = 50) List<@Size(max = 300) String> dataLines) {}
}
