package com.openlab.qualitos.quality.supplier;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** La fiche NC fournisseur : ce qu'elle dit, et qu'elle tient sur plusieurs pages si besoin. */
class SupplierNcReportRendererTest {

    private final SupplierNcReportRenderer renderer = new SupplierNcReportRenderer();

    private SupplierNcReport report(String description, List<SupplierNcReport.Attachment> attachments) {
        return new SupplierNcReport("ACME-01", "Acme Industrie", "PN-7781-A", 120, "LOT-42",
                LocalDate.parse("2026-10-01"), null,
                NonConformitySeverity.CRITICAL, NonConformityStatus.IN_REVIEW,
                true, false, description, null, attachments,
                Instant.parse("2026-10-24T08:30:00Z"));
    }

    private static String text(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void ecritLIdentificationLe8DEtLesPiecesJointes() throws Exception {
        byte[] pdf = renderer.render(report("Cote Ø12 hors tolérance — « bavure » sur 3 pièces", List.of(
                new SupplierNcReport.Attachment(SupplierDocumentCategory.EIGHT_D, "8D-acme.pdf", 1_258_291,
                        Instant.parse("2026-10-20T09:00:00Z")),
                new SupplierNcReport.Attachment(SupplierDocumentCategory.CAPA_EVIDENCE, null, 300,
                        Instant.parse("2026-10-21T09:00:00Z")))));

        String t = text(pdf);
        assertThat(t).contains("Fiche de non-conformité fournisseur")
                .contains("ACME-01 — Acme Industrie")
                .contains("PN-7781-A").contains("120").contains("LOT-42")
                .contains("Critique").contains("En analyse")
                .contains("8D demandé").contains("Oui").contains("Non")
                .contains("hors tolérance")
                .contains("Pièces jointes (2)")
                .contains("8D fournisseur — 8D-acme.pdf (1,2 Mo")
                .contains("Preuve CAPA — sans nom (1 Ko")
                .contains("Non renseigné")   // résolution vide
                .contains("Édité le 24/10/2026 10:30");
    }

    @Test
    void sansPieceJointe_leDit() throws Exception {
        assertThat(text(renderer.render(report(null, List.of())))).contains("Pièces jointes (0)");
    }

    @Test
    void longTexte_passeALaPageSuivante() throws Exception {
        String longText = "Ligne de constat détaillée sur le lot reçu au magasin. ".repeat(400);
        try (PDDocument doc = Loader.loadPDF(renderer.render(report(longText, List.of())))) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }

    @Test
    void fournisseurSansCode_etCaracteresHorsJeu() throws Exception {
        SupplierNcReport r = new SupplierNcReport(null, "Sans code", null, null, null,
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-05"),
                NonConformitySeverity.MINOR, NonConformityStatus.RESOLVED, false, true,
                "Émoji 🚀 et idéogramme 漢", "Lot trié\tet remplacé", null, Instant.parse("2026-10-24T08:30:00Z"));
        String t = text(renderer.render(r));
        assertThat(t).contains("Sans code").contains("05/10/2026").contains("Résolue").contains("Mineure");
    }

    @Test
    void outils() {
        assertThat(SupplierNcReportRenderer.size(2048)).isEqualTo("2 Ko");
        assertThat(SupplierNcReportRenderer.sanitize(null)).isEmpty();
        assertThat(SupplierNcReportRenderer.sanitize("’“”œŒ")).isEqualTo("'\"\"oeOE");
        assertThat(SupplierNcReportRenderer.wrap("  ")).containsExactly("");
        assertThat(SupplierNcReportRenderer.wrap("x".repeat(150))).hasSize(2);
    }
}
