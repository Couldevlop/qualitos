package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDDiscipline;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le rendu du rapport 8D.
 *
 * <p>Le test qui compte vraiment ici est celui du DÉTERMINISME : l'empreinte
 * scellée à l'émission porte sur les octets du PDF, et le service refuse de remettre
 * un document dont le rendu ne retombe pas sur cette empreinte. Un rendu qui
 * dépendrait de l'heure ou d'un identifiant aléatoire rendrait donc le
 * téléchargement impossible dès la seconde d'après — panne muette et incompréhensible.
 */
class PdfBoxEightDRenderAdapterTest {

    private final PdfBoxEightDRenderAdapter renderer = new PdfBoxEightDRenderAdapter();

    private static final String URL = "https://app.qualitos.io/api/v1/nc/public/8d/ABCDEFGHIJKLMNOP/verify";

    @Test
    void deux_rendus_du_meme_rapport_donnent_exactement_les_memes_octets() {
        EightDSnapshot snapshot = snapshot(true);

        byte[] premier = renderer.render(snapshot, URL);
        byte[] second = renderer.render(snapshot, URL);

        assertThat(premier).isEqualTo(second);
    }

    @Test
    void le_document_rendu_est_un_pdf_non_vide() {
        byte[] pdf = renderer.render(snapshot(true), URL);

        assertThat(pdf).hasSizeGreaterThan(1000);
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void une_discipline_sans_source_le_dit_dans_le_pdf() throws IOException {
        // D1, D3 et D8 vides : le document doit l'annoncer, pas laisser trois blancs.
        String texte = texteDe(renderer.render(snapshot(false), URL));

        assertThat(texte).contains("PARTIAL REPORT");
        assertThat(texte).contains("D1, D3, D8");
        assertThat(texte).contains("Not filled in.");
        assertThat(texte).contains("cette discipline se saisit");
    }

    @Test
    void le_rapport_complet_ne_porte_aucune_mention_de_partiel() throws IOException {
        String texte = texteDe(renderer.render(snapshot(true), URL));

        assertThat(texte).doesNotContain("PARTIAL REPORT");
        assertThat(texte).contains("NC-2026-0007");
        assertThat(texte).contains("D4 - Root cause");
        assertThat(texte).contains("Integrity & verification");
    }

    @Test
    void les_huit_disciplines_figurent_dans_l_ordre_de_la_methode() throws IOException {
        String texte = texteDe(renderer.render(snapshot(true), URL));

        int precedent = -1;
        for (EightDDiscipline discipline : EightDDiscipline.values()) {
            int position = texte.indexOf(discipline.code() + " - ");
            assertThat(position)
                    .as("la discipline %s doit figurer", discipline.code())
                    .isGreaterThan(precedent);
            precedent = position;
        }
    }

    @Test
    void un_contenu_long_deborde_sur_plusieurs_pages_sans_rien_perdre() throws IOException {
        List<String> lignes = new ArrayList<>();
        for (int i = 1; i <= 120; i++) {
            lignes.add("Action " + i + " : une ligne de contenu suffisamment longue pour que"
                    + " le repli de ligne entre en jeu et que la pagination soit exercee.");
        }
        EightDSnapshot snapshot = new EightDSnapshot(
                "NC-2026-0008", "Fuite de pompe", "tenant", "01/01/2026 00:00 UTC", "Ada", false,
                sections(lignes));

        byte[] pdf = renderer.render(snapshot, URL);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
        assertThat(texteDe(pdf)).contains("Action 120");
    }

    // ---------- fixtures ----------

    private EightDSnapshot snapshot(boolean complet) {
        List<EightDSnapshot.Section> sections = new ArrayList<>();
        for (EightDDiscipline discipline : EightDDiscipline.values()) {
            boolean servi = complet || !discipline.estSaisie();
            sections.add(new EightDSnapshot.Section(
                    discipline.code(), discipline.titre(), servi,
                    servi ? "Agrégé depuis : source de démonstration"
                          : "Aucune source dans la plateforme — cette discipline se saisit",
                    servi ? List.of("Contenu de " + discipline.code(),
                                    "    sous-point indenté de " + discipline.code())
                          : List.of()));
        }
        return new EightDSnapshot("NC-2026-0007", "Fuite au presse-étoupe", "tenant-demo",
                "13/09/2026 10:00 UTC", "Ada Lovelace", !complet, sections);
    }

    private List<EightDSnapshot.Section> sections(List<String> lignesDeLaPremiere) {
        List<EightDSnapshot.Section> sections = new ArrayList<>();
        for (EightDDiscipline discipline : EightDDiscipline.values()) {
            sections.add(new EightDSnapshot.Section(
                    discipline.code(), discipline.titre(), true, "Source",
                    discipline == EightDDiscipline.D5 ? lignesDeLaPremiere : List.of("ok")));
        }
        return sections;
    }

    private String texteDe(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
