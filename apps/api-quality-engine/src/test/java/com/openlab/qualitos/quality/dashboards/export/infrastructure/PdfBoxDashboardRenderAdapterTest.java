package com.openlab.qualitos.quality.dashboards.export.infrastructure;

import com.openlab.qualitos.quality.dashboards.export.domain.DashboardExportModel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfBoxDashboardRenderAdapterTest {

    final PdfBoxDashboardRenderAdapter adapter = new PdfBoxDashboardRenderAdapter();

    DashboardExportModel model(List<DashboardExportModel.Widget> widgets) {
        return new DashboardExportModel(
                "Tableau Qualité Exécutif", "11111111-1111-1111-1111-111111111111",
                "22222222-2222-2222-2222-222222222222",
                Instant.parse("2026-06-22T08:30:00Z"),
                widgets, "abcDEF012345_-xy");
    }

    @Test
    void render_producesNonEmptyParsablePdf() throws Exception {
        byte[] pdf = adapter.render(model(List.of(
                new DashboardExportModel.Widget("CAPA closure", "kpi",
                        List.of("Cible: 30 j", "Réalisé: 24 j")),
                new DashboardExportModel.Widget("NC trend", "line", List.of("Mars: 12", "Avril: 9")))),
                "https://app.qualitos.io/api/v1/dashboards/public/exports/abcDEF012345_-xy/verify");

        assertThat(pdf).isNotEmpty();
        // PDF magic header.
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void render_emptyWidgets_stillProducesPdf() throws Exception {
        byte[] pdf = adapter.render(model(List.of()), "https://x/verify");
        assertThat(pdf).isNotEmpty();
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void render_manyWidgets_clampsToSinglePageWithoutCrash() throws Exception {
        var widgets = new java.util.ArrayList<DashboardExportModel.Widget>();
        for (int i = 0; i < 60; i++) {
            widgets.add(new DashboardExportModel.Widget("Widget " + i, "bar",
                    List.of("ligne a", "ligne b", "ligne c")));
        }
        byte[] pdf = adapter.render(model(widgets), "https://x/verify");
        assertThat(pdf).isNotEmpty();
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }
}
