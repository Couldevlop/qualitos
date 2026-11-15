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

    // ---------- les figures du D4 ----------

    /**
     * L'arete de poisson se TRACE : l'enonce dans la tete, et chaque categorie
     * sur sa branche. Le texte du PDF doit donc porter ces etiquettes, qui ne
     * venaient d'aucune ligne de la discipline.
     */
    @Test
    void l_arete_de_poisson_porte_son_enonce_et_ses_branches() throws IOException {
        String texte = texteDe(renderer.render(avecFigure(areteDeDemo()), URL));

        // Le rendu ramene au jeu WinAnsi des polices standard : accents degrades.
        assertThat(texte).contains("Ishikawa - Fuite au presse-etoupe");
        assertThat(texte).contains("MATERIEL");
        assertThat(texte).contains("METHODE");
        assertThat(texte).contains("Joint use");
        assertThat(texte).contains("Pas de plan");
    }

    /**
     * La descente numerote ses rangs, et NOMME la cause racine : c'est ce qui
     * distingue une descente conclue d'une descente interrompue.
     */
    @Test
    void la_descente_des_pourquoi_numerote_ses_rangs_et_nomme_la_cause_racine() throws IOException {
        String texte = texteDe(renderer.render(avecFigure(echelleDeDemo()), URL));

        assertThat(texte).contains("5 whys - Fuite constatee");
        assertThat(texte).contains("Why 1");
        assertThat(texte).contains("Why 2");
        assertThat(texte).contains("Root cause");
        assertThat(texte).contains("Absence de plan preventif");
    }

    /**
     * Un diagramme ouvert puis laisse vide existe. Le dire vaut mieux que
     * tracer une epine nue, qu'on prendrait pour un defaut de rendu.
     */
    @Test
    void une_figure_sans_noeud_le_dit_au_lieu_de_tracer_une_epine_nue() throws IOException {
        EightDSnapshot.Figure vide = new EightDSnapshot.Figure(
                EightDSnapshot.FigureKind.FISHBONE, "Fuite au presse-\u00e9toupe", List.of());

        String texte = texteDe(renderer.render(avecFigure(vide), URL));

        assertThat(texte).contains("Opened, but left empty.");
    }

    /**
     * Une discipline SANS LIGNE mais avec une figure n'est pas vide : sans
     * cette distinction, le D4 d'un ecart dont la cause s'est cherchee en
     * Ishikawa affichait « Not filled in. » au-dessus de son propre trace.
     */
    @Test
    void une_discipline_sans_ligne_mais_avec_figure_ne_se_dit_pas_vide() throws IOException {
        String texte = texteDe(renderer.render(avecFigure(areteDeDemo()), URL));

        assertThat(texte).doesNotContain("Not filled in.");
    }

    /** Le determinisme vaut aussi pour les figures -- sinon le sceau tombe a faux. */
    @Test
    void deux_rendus_d_une_figure_donnent_les_memes_octets() {
        EightDSnapshot snapshot = avecFigure(areteDeDemo());

        assertThat(renderer.render(snapshot, URL)).isEqualTo(renderer.render(snapshot, URL));
    }

    /**
     * L'INVARIANT QUI PROTEGE LES RAPPORTS DEJA EMIS. Leur instantane a ete
     * scelle sans figure ; relu aujourd'hui, il doit rendre exactement les
     * memes octets qu'avant, sinon leur empreinte ne retombe plus juste et le
     * telechargement casse en silence.
     */
    @Test
    void un_rapport_scelle_avant_les_figures_rend_les_memes_octets_qu_avant() {
        EightDSnapshot sansChamp = snapshot(true);
        EightDSnapshot figuresVides = new EightDSnapshot(
                sansChamp.ncReference(), sansChamp.ncTitle(), sansChamp.tenantLabel(),
                sansChamp.issuedAtText(), sansChamp.issuedByName(), sansChamp.partial(),
                sansChamp.sections().stream()
                        .map(s -> new EightDSnapshot.Section(s.code(), s.title(), s.sourced(),
                                s.sourceLabel(), s.lines(), List.of()))
                        .toList());

        assertThat(renderer.render(figuresVides, URL)).isEqualTo(renderer.render(sansChamp, URL));
    }

    /**
     * Une branche chargee ne doit pas empiler ses causes les unes sur les
     * autres : la hauteur de la figure se calcule sur le cote le plus charge.
     * On verifie que les huit causes sortent toutes dans le texte.
     */
    @Test
    void une_branche_chargee_sort_toutes_ses_causes() throws IOException {
        List<EightDSnapshot.Node> noeuds = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            noeuds.add(new EightDSnapshot.Node("MATERIEL", "Cause numero " + i, 1));
        }
        EightDSnapshot.Figure chargee = new EightDSnapshot.Figure(
                EightDSnapshot.FigureKind.FISHBONE, "Fuite", noeuds);

        String texte = texteDe(renderer.render(avecFigure(chargee), URL));

        for (int i = 1; i <= 8; i++) {
            assertThat(texte).as("la cause %s doit figurer", i).contains("Cause numero " + i);
        }
    }

    /**
     * Une descente longue traverse la coupure de page sans perdre un rang : un
     * « pourquoi » a cheval perdrait ce qui le relie a la question precedente.
     */
    @Test
    void une_descente_longue_traverse_la_coupure_de_page_sans_perdre_un_rang() throws IOException {
        List<EightDSnapshot.Node> noeuds = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            noeuds.add(new EightDSnapshot.Node("WHY",
                    "Reponse numero " + i + " : une explication assez longue pour que le repli"
                    + " de ligne entre en jeu et que la pagination soit exercee.", i));
        }
        EightDSnapshot.Figure longue = new EightDSnapshot.Figure(
                EightDSnapshot.FigureKind.WHY_CHAIN, "Fuite", noeuds);

        byte[] pdf = renderer.render(avecFigure(longue), URL);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
        assertThat(texteDe(pdf)).contains("Why 30");
    }

    /**
     * UNE CAUSE LONGUE SE REPLIE, elle ne se TRONQUE pas.
     *
     * <p>Le premier trace coupait a 26 caracteres : « Joint use au-dela de sa
     * duree de vie » sortait « Joint use au-dela de sa », ampute en plein mot et
     * sans rien qui le signale -- un lecteur du rapport ne pouvait pas savoir
     * qu'il manquait la moitie de la cause. Le repli se mesure desormais a la
     * police, donc le DERNIER mot doit se retrouver dans le PDF.
     */
    @Test
    void une_cause_longue_se_replie_au_lieu_d_etre_tronquee() throws IOException {
        EightDSnapshot.Figure figure = new EightDSnapshot.Figure(
                EightDSnapshot.FigureKind.FISHBONE, "Fuite", List.of(
                        new EightDSnapshot.Node("MATERIEL",
                                "Joint use au-dela de sa duree de vie nominale", 1)));

        String texte = texteDe(renderer.render(avecFigure(figure), URL));

        assertThat(texte).contains("Joint use");
        assertThat(texte).as("le dernier mot de la cause ne doit pas disparaitre")
                .contains("nominale");
    }

    // ---------- fixtures ----------

    /** Un instantane complet dont le D4 porte la figure donnee, et aucune ligne. */
    private EightDSnapshot avecFigure(EightDSnapshot.Figure figure) {
        List<EightDSnapshot.Section> sections = new ArrayList<>();
        for (EightDDiscipline discipline : EightDDiscipline.values()) {
            boolean d4 = discipline == EightDDiscipline.D4;
            sections.add(new EightDSnapshot.Section(
                    discipline.code(), discipline.titre(), true, "Source",
                    d4 ? List.of() : List.of("Contenu de " + discipline.code()),
                    d4 ? List.of(figure) : List.of()));
        }
        return new EightDSnapshot("NC-2026-0007", "Fuite au presse-\u00e9toupe", "tenant-demo",
                "13/09/2026 10:00 UTC", "Ada Lovelace", false, sections);
    }

    private EightDSnapshot.Figure areteDeDemo() {
        return new EightDSnapshot.Figure(EightDSnapshot.FigureKind.FISHBONE,
                "Fuite au presse-\u00e9toupe", List.of(
                        new EightDSnapshot.Node("MATERIEL", "Joint us\u00e9", 1),
                        new EightDSnapshot.Node("METHODE", "Pas de plan de remplacement", 1)));
    }

    private EightDSnapshot.Figure echelleDeDemo() {
        return new EightDSnapshot.Figure(EightDSnapshot.FigureKind.WHY_CHAIN,
                "Fuite constat\u00e9e", List.of(
                        new EightDSnapshot.Node("WHY", "Le joint a c\u00e9d\u00e9", 1),
                        new EightDSnapshot.Node("WHY", "Il n'a jamais \u00e9t\u00e9 remplac\u00e9", 2),
                        new EightDSnapshot.Node("ROOT", "Absence de plan pr\u00e9ventif", 3)));
    }

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
