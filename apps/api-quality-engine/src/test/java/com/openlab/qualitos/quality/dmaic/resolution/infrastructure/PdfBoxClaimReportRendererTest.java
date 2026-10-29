package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import com.openlab.qualitos.quality.dmaic.resolution.application.ClaimReport;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le rapport client suit la feuille du classeur : en-tête, puis les cinq
 * étapes ; une rubrique vide se dit, un long texte passe à la page suivante.
 */
class PdfBoxClaimReportRendererTest {

    final PdfBoxClaimReportRenderer renderer = new PdfBoxClaimReportRenderer();

    static ClaimReport rapport(String surveillance) {
        return new ClaimReport("Acme Industrie", "CL-2026-042", "Fissures carter", "Alice Martin",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), "1 - Sévère",
                "V10 - Defect - Linear, cracks, tears, seams, broken, etc. and a much longer tail that is cut",
                List.of(new ClaimReport.Answer("Quel est le problème et comment a-t-il été identifié ?",
                                "Fissure à la base du carter"),
                        new ClaimReport.Answer("Quelle est la quantité ?", null)),
                null, "Tri à 100 % du stock", "Isolement des lots",
                List.of(new ClaimReport.Answer("Machines", "Outil usé")),
                List.of(new ClaimReport.Why("Pourquoi le problème est-il survenu ?", "Bavure",
                        List.of("Outil usé", "Absence de compteur"), true, "Compteur de coups")),
                "Absence de compteur", "Nouvel outil",
                List.of(new ClaimReport.Action("Changer l'outil", "Bob", "En cours", LocalDate.of(2026, 10, 5),
                        null, 50)),
                surveillance);
    }

    static String texte(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void lesCinqEtapesEtLEnTeteSontEcrits() throws IOException {
        String t = texte(renderer.render(rapport("Contrôle horaire")));

        assertThat(t).contains("Rapport d'action corrective client DMAIC", "Acme Industrie", "CL-2026-042",
                "Alice Martin", "01/10/2026", "20/10/2026", "1 - Sévère", "Fissure à la base du carter",
                "Tri à 100 % du stock", "Isolement des lots", "Machines : Outil usé", "Pourquoi 2 : Absence de compteur",
                "cause racine", "Compteur de coups", "Nouvel outil", "1. Changer l'outil", "Responsable : Bob",
                "05/10/2026", "Avancement : 50 %", "Contrôle horaire", "étape 1", "étape 2", "étape 3", "étape 4",
                "étape 5", "Plan d'action temporaire", "Diagramme d'Ishikawa", "5 pourquoi", "Plan d'actions",
                "Le plan de surveillance élimine-t-il");
        assertThat(t).contains(PdfBoxClaimReportRenderer.EMPTY);
    }

    @Test
    void unLongTextePasseALaPageSuivante() throws IOException {
        String long_ = ("Une ligne de surveillance détaillée qui revient souvent. ").repeat(400);

        byte[] pdf = renderer.render(rapport(long_));

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }

    @Test
    void unRapportViergeSortQuandMeme() throws IOException {
        String t = texte(renderer.render(new ClaimReport(null, null, "Projet", null, null, null, null, null,
                List.of(), null, null, null, List.of(), List.of(), null, null, List.of(), null)));

        assertThat(t).contains("Projet", PdfBoxClaimReportRenderer.EMPTY);
    }

    @Test
    void leTexteEstRameneAuJeuDesPolicesStandard() {
        assertThat(PdfBoxClaimReportRenderer.sanitize(null)).isEmpty();
        assertThat(PdfBoxClaimReportRenderer.sanitize("Été à Noël")).isEqualTo("Été à Noël");
        assertThat(PdfBoxClaimReportRenderer.sanitize("l’œuvre “x”\tfin")).isEqualTo("l'oeuvre \"x\" fin");
        assertThat(PdfBoxClaimReportRenderer.sanitize("a—b…")).isEqualTo("a—b…");
        assertThat(PdfBoxClaimReportRenderer.sanitize("Œ 漢")).isEqualTo("OE ?");
    }

    @Test
    void lesLignesSontCoupeesAuMot() {
        assertThat(PdfBoxClaimReportRenderer.wrap("  ")).containsExactly("");
        assertThat(PdfBoxClaimReportRenderer.wrap("court")).containsExactly("court");
        List<String> parts = PdfBoxClaimReportRenderer.wrap(("mot ").repeat(60));
        assertThat(parts).hasSizeGreaterThan(1).allSatisfy(p -> assertThat(p.length()).isLessThanOrEqualTo(100));
        assertThat(PdfBoxClaimReportRenderer.wrap("x".repeat(250))).hasSize(3);
    }
}
