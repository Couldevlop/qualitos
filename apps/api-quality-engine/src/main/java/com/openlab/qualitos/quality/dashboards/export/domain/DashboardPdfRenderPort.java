package com.openlab.qualitos.quality.dashboards.export.domain;

/**
 * Port — renders a dashboard into a self-contained, printable PDF document.
 *
 * <p>The concrete adapter (Apache PDFBox + ZXing QR, infrastructure layer)
 * lays out the title, tenant metadata, the widget table and the verification
 * footer with an embedded QR code pointing at the public verify URL. The domain
 * stays free of any PDF/QR library dependency (hexagonal arch — ArchUnit).
 */
public interface DashboardPdfRenderPort {

    /**
     * @param model   the export view-model (title, metadata, widgets, integrity facts)
     * @param verifyUrl absolute URL encoded in the QR code (public verify endpoint)
     * @return the rendered PDF as bytes (non-empty)
     */
    byte[] render(DashboardExportModel model, String verifyUrl);
}
