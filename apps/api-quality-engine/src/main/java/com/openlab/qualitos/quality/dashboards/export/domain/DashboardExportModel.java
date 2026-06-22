package com.openlab.qualitos.quality.dashboards.export.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable view-model handed to the {@link DashboardPdfRenderPort}. Pure data,
 * no framework deps. Built by the application service from the persisted layout
 * (tenant-scoped) plus the optional widget snapshots supplied in the request.
 *
 * @param dashboardName  the dashboard title
 * @param tenantId       tenant UUID (from JWT) — printed in metadata
 * @param dashboardId    dashboard UUID
 * @param generatedAt    generation timestamp
 * @param widgets        one row per widget (title, type, snapshot value lines)
 * @param verificationCode  short code embedded in the QR + footer
 *
 * <p>The model deliberately does NOT carry the SHA-256: a document cannot embed
 * the fingerprint of its own final bytes. The fingerprint is computed over the
 * rendered bytes afterwards, then signed and anchored. The PDF instead carries
 * the (stable) verification code + QR, which resolve the fingerprint server-side.
 */
public record DashboardExportModel(
        String dashboardName,
        String tenantId,
        String dashboardId,
        Instant generatedAt,
        List<Widget> widgets,
        String verificationCode) {

    public DashboardExportModel {
        Objects.requireNonNull(dashboardName, "dashboardName");
        Objects.requireNonNull(generatedAt, "generatedAt");
        Objects.requireNonNull(verificationCode, "verificationCode");
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
    }

    /**
     * A single widget rendered as a table row.
     *
     * @param title    widget title
     * @param type     widget type (kpi, line, bar…)
     * @param dataLines human-readable snapshot lines (e.g. "Cible: 30 j", "Réalisé: 24 j")
     */
    public record Widget(String title, String type, List<String> dataLines) {
        public Widget {
            Objects.requireNonNull(title, "title");
            type = type == null ? "" : type;
            dataLines = dataLines == null ? List.of() : List.copyOf(dataLines);
        }
    }
}
