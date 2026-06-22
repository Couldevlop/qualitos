package com.openlab.qualitos.quality.dashboards.export.application;

import java.time.Instant;
import java.util.List;

/**
 * Application DTOs for the signed dashboard PDF export feature (§7.3 / §7.4).
 */
public final class DashboardExportDto {

    private DashboardExportDto() {}

    /**
     * Export command. The dashboard id comes from the path; the tenant/user from
     * the JWT. The optional widget snapshots let the caller embed the exact data
     * displayed on screen; when omitted the export still lists the widgets.
     *
     * @param widgets per-widget snapshots to print (may be empty/null)
     */
    public record ExportCommand(List<WidgetSnapshot> widgets) {
        public ExportCommand {
            widgets = widgets == null ? List.of() : List.copyOf(widgets);
        }
    }

    /**
     * A widget snapshot supplied by the front-end (already-computed view data).
     *
     * @param title     widget title
     * @param type      widget type
     * @param dataLines human-readable data rows
     */
    public record WidgetSnapshot(String title, String type, List<String> dataLines) {
        public WidgetSnapshot {
            dataLines = dataLines == null ? List.of() : List.copyOf(dataLines);
        }
    }

    /**
     * Result of an export: the rendered PDF bytes plus integrity metadata. The
     * controller streams {@code pdf} and may surface the metadata in headers.
     */
    public record ExportResult(
            byte[] pdf,
            String fileName,
            String verificationCode,
            String sha256Hex,
            String anchorTxRef,
            Instant generatedAt) {}

    /**
     * Strict public verification view. Carries NO tenant-scoped business data —
     * only integrity facts (OWASP A01: no data disclosure on the public path).
     *
     * @param valid          whether the stored signature re-validates over the fingerprint
     * @param verificationCode the queried code (echoed)
     * @param sha256Hex      the fingerprint of the originally rendered PDF
     * @param anchorTxRef    the blockchain anchor reference
     * @param dashboardName  the dashboard title (printed on the public certificate of integrity)
     * @param generatedAt    when the export was produced
     */
    public record VerificationResult(
            boolean valid,
            String verificationCode,
            String sha256Hex,
            String anchorTxRef,
            String dashboardName,
            Instant generatedAt) {

        /** Unknown code → not valid, no details. */
        public static VerificationResult unknown(String code) {
            return new VerificationResult(false, code, null, null, null, null);
        }
    }
}
