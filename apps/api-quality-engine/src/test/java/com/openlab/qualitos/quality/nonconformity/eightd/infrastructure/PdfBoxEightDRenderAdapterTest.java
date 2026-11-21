package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDDiscipline;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
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
        assertThat(texte).contains("D4 - Cause racine");
        assertThat(texte).contains("Integrity & verification");
    }

    /**
     * LE DOCUMENT PARLE LA LANGUE DE SON INSTANTANE, et le document neuf parle
     * francais : le rapport sortait « 8D Report », « Issued on », « Not filled
     * in. » sous une interface francaise, et c'est lui qui part chez le client.
     */
    @Test
    void un_instantane_en_francais_rend_un_document_francais() throws IOException {
        String texte = texteDe(renderer.render(enFrancais(snapshot(false)), URL));

        assertThat(texte).contains("Rapport 8D");
        assertThat(texte).contains("Non-conformité NC-2026-0007");
        assertThat(texte).contains("Émis le");
        assertThat(texte).contains("Non renseignée.");
        assertThat(texte).contains("RAPPORT PARTIEL - disciplines sans contenu :");
        assertThat(texte).contains("Intégrité et vérification");
        assertThat(texte).doesNotContain("8D Report");
        assertThat(texte).doesNotContain("Issued on");
        assertThat(texte).doesNotContain("Not filled in.");
    }

    /**
     * L'INVARIANT QUI PROTEGE LES RAPPORTS DEJA EMIS. Leur instantane a ete
     * scelle avant que le document parle francais, et son empreinte porte sur
     * les octets du PDF : relu aujourd'hui, il doit rendre les mots d'alors,
     * sinon l'empreinte ne retombe plus juste et le telechargement casse en
     * silence.
     */
    @Test
    void un_rapport_scelle_avant_le_francais_garde_ses_mots() throws IOException {
        EightDSnapshot sansLangue = snapshot(true);

        String texte = texteDe(renderer.render(sansLangue, URL));

        assertThat(sansLangue.langue()).isNull();
        assertThat(texte).contains("8D Report");
        assertThat(texte).contains("Issued on");
        assertThat(texte).doesNotContain("Rapport 8D");
    }

    /**
     * LES ACCENTS DU RAPPORT SORTENT. Le rendu les degradait, en invoquant
     * l'impuissance des polices standard -- qui est fausse : elles sont en
     * WinAnsiEncoding, qui porte tout le francais. Un 8D part chez le client,
     * et « Ecart constate » au lieu d'« Écart constaté » abime une preuve.
     */
    @Test
    void les_accents_du_contenu_sortent_tels_quels() throws IOException {
        String texte = texteDe(renderer.render(snapshotDe(sections(List.of(
                "Écart constaté : réclamation d'un client — cédille ç, ligature œuvre",
                "Prévention : contrôle à 100 %, où l'opérateur vise l'arête"))), URL));

        assertThat(texte).contains("Écart constaté : réclamation d'un client");
        assertThat(texte).contains("cédille ç, ligature œuvre");
        assertThat(texte).contains("Prévention : contrôle à 100 %");
        assertThat(texte).contains("où l'opérateur vise l'arête");
    }

    /**
     * CE QUE LES POLICES NE SAVENT PAS ECRIRE NE FAIT PAS ECHOUER L'EMISSION.
     * PDFBox leve une exception sur un caractere hors du jeu de la police ; la
     * remettre au visage de l'utilisateur ferait perdre le document pour un
     * signe. On perd le signe, on garde la preuve.
     */
    @Test
    void un_caractere_hors_du_jeu_des_polices_est_remplace_sans_casser_le_rendu()
            throws IOException {
        byte[] pdf = renderer.render(snapshotDe(sections(List.of(
                "Cible ≤ 500 ppm, tendance → baisse, repere 中"))), URL);

        String texte = texteDe(pdf);
        assertThat(pdf).isNotEmpty();
        assertThat(texte).contains("Cible ? 500 ppm, tendance ? baisse, repere ?");
        assertThat(texte).doesNotContain("≤");
    }

    /** Le meme instantane, avec la langue du document neuf. */
    private EightDSnapshot enFrancais(EightDSnapshot snapshot) {
        return new EightDSnapshot(snapshot.ncReference(), snapshot.ncTitle(),
                snapshot.tenantLabel(), snapshot.issuedAtText(), snapshot.issuedByName(),
                snapshot.partial(), snapshot.sections(), "fr");
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

        // Les polices standard portent le francais : les accents de la source sortent
        assertThat(texte).contains("Ishikawa - Fuite au presse-étoupe");
        assertThat(texte).contains("MATERIEL");
        assertThat(texte).contains("METHODE");
        assertThat(texte).contains("Joint usé");
        assertThat(texte).contains("Pas de plan");
    }

    /**
     * La descente numerote ses rangs, et NOMME la cause racine : c'est ce qui
     * distingue une descente conclue d'une descente interrompue.
     */
    @Test
    void la_descente_des_pourquoi_numerote_ses_rangs_et_nomme_la_cause_racine() throws IOException {
        String texte = texteDe(renderer.render(avecFigure(echelleDeDemo()), URL));

        assertThat(texte).contains("5 whys - Fuite constatée");
        assertThat(texte).contains("Why 1");
        assertThat(texte).contains("Why 2");
        assertThat(texte).contains("Root cause");
        assertThat(texte).contains("Absence de plan préventif");
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


    // ---------- une figure ne se coupe pas, le pied n'ecrase pas le contenu ----

    /**
     * UNE DESCENTE NE SE SEPARE PAS DE SA CONCLUSION.
     *
     * <p>Le pied d'integrite voyait sa hauteur reservee sur TOUTES les pages
     * alors qu'il ne s'ecrit que sur la derniere : chaque page perdait un tiers
     * de sa hauteur, et l'echelle des pourquoi se coupait bien avant le bas. On
     * a vu ainsi la cause racine atterrir seule en haut de la page suivante,
     * detachee du raisonnement dont elle est la conclusion -- c'est-a-dire
     * exactement ce qu'un auditeur vient lire.
     *
     * <p>On fait varier la longueur du contenu qui precede : la coupure tombe
     * ailleurs a chaque tour, et l'invariant doit tenir partout.
     */
    @Test
    void une_descente_et_sa_cause_racine_restent_sur_la_meme_page() throws IOException {
        for (int n = 0; n <= 24; n += 2) {
            byte[] pdf = renderer.render(avecRemplissage(echelleDeDemo(), n), URL);
            // Le LIBELLE du noeud, pas l'etiquette « Root cause » : celle-ci est
            // aussi le titre du D4, qui peut se trouver sur une autre page.
            int page = pageQuiPorte(pdf, "Le joint a cédé");
            assertThat(pageQuiPorte(pdf, "Absence de plan préventif"))
                    .as("avec %s lignes avant, la cause racine doit rester avec sa descente", n)
                    .isEqualTo(page);
        }
    }

    /**
     * Et l'arete non plus : une epine dont la moitie des branches passe a la
     * page suivante ne montre plus rien. Les deux categories doivent se lire
     * ensemble, quel que soit l'endroit ou tombe la coupure.
     */
    @Test
    void une_arete_ne_se_coupe_pas_entre_deux_pages() throws IOException {
        for (int n = 0; n <= 24; n += 2) {
            byte[] pdf = renderer.render(avecRemplissage(areteDeDemo(), n), URL);
            assertThat(pageQuiPorte(pdf, "METHODE"))
                    .as("avec %s lignes avant, l'arete doit tenir d'un bloc", n)
                    .isEqualTo(pageQuiPorte(pdf, "MATERIEL"));
        }
    }

    /**
     * MEME TROP LONGUE POUR UNE PAGE, une descente ne laisse pas sa cause
     * racine seule : les deux derniers barreaux se posent ensemble. Une
     * conclusion sans la marche qui la precede n'est plus qu'une affirmation.
     */
    @Test
    void une_descente_trop_longue_ne_laisse_pas_sa_cause_racine_seule() throws IOException {
        List<EightDSnapshot.Node> noeuds = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            noeuds.add(new EightDSnapshot.Node("WHY",
                    "Reponse numero " + i + " : une explication assez longue pour que le repli"
                    + " de ligne entre en jeu et que la pagination soit exercee.", i));
        }
        noeuds.add(new EightDSnapshot.Node("ROOT", "La gamme ignore ce composant", 41));
        EightDSnapshot.Figure longue = new EightDSnapshot.Figure(
                EightDSnapshot.FigureKind.WHY_CHAIN, "Fuite", noeuds);

        byte[] pdf = renderer.render(avecFigure(longue), URL);

        assertThat(pageQuiPorte(pdf, "Reponse numero 40"))
                .as("la cause racine ne doit pas ouvrir une page a elle seule")
                .isEqualTo(pageQuiPorte(pdf, "La gamme ignore ce composant"));
    }

    /**
     * LE PIED D'INTEGRITE N'ECRIT JAMAIS PAR-DESSUS LE CONTENU.
     *
     * <p>C'est la contrepartie de la correction precedente : le contenu descend
     * desormais jusqu'a la marge basse, la ou il s'arretait un tiers de page
     * plus haut. Le pied, lui, s'ecrit a hauteur fixe au bas de la DERNIERE
     * page. S'ils se rencontrent, le document perd a la fois la preuve et ce
     * qu'elle atteste -- il prend donc une page a lui.
     *
     * <p>On mesure les ordonnees reelles du texte : c'est le seul moyen de voir
     * un chevauchement, qu'une lecture a plat du PDF ne montrerait pas (les
     * deux textes s'extraient, superposes ou non).
     *
     * <p>Seule la page du pied est jugee. Ailleurs, le contenu a desormais le
     * droit de descendre jusqu'a la marge : c'est tout l'objet de la
     * correction, et le lui reprocher reviendrait a refaire le defaut.
     */
    @Test
    void le_pied_d_integrite_n_ecrit_jamais_par_dessus_le_contenu() throws IOException {
        for (int n = 1; n <= 40; n += 3) {
            List<String> lignes = new ArrayList<>();
            for (int i = 1; i <= n; i++) {
                lignes.add("Ligne de contenu numero " + i + " : assez longue pour que le repli"
                        + " entre en jeu et que la coupure tombe a un endroit different.");
            }
            byte[] pdf = renderer.render(new EightDSnapshot(
                    "NC-2026-0009", "Fuite", "tenant", "01/01/2026 00:00 UTC", "Ada", false,
                    sections(lignes)), URL);

            assertThat(contenuSousLeFilet(pdf))
                    .as("avec %s lignes, aucun contenu ne doit passer sous le filet du pied", n)
                    .isEmpty();
        }
    }


    // ---------- les tableaux d'actions ----------

    /**
     * LE DOCUMENT EMIS SE PRESENTE COMME L'ECRAN. L'ecran montre les actions
     * decidees en colonnes -- nature, intitule, porteur, echeance, avancement,
     * preuve ; le PDF les enumerait en phrases. Les memes donnees, mais plus
     * comparables d'une ligne a l'autre, alors que c'est tout ce qu'on vient
     * faire dans ces disciplines : voir ce qui reste ouvert, ce qui est en
     * retard, ce qui n'est pas prouve.
     */
    @Test
    void le_tableau_des_actions_sort_entier_dans_le_pdf() throws IOException {
        byte[] pdf = renderer.render(avecTableau(tableauDeDemo()), URL);

        String texte = texteDe(pdf);
        assertThat(texte).contains("Actions - Dossier A");
        // Les en-tetes, en capitales comme a l'ecran.
        assertThat(texte).contains("KIND").contains("ACTION").contains("OWNER")
                .contains("DUE DATE").contains("STATUS").contains("EVIDENCE");
        // Et chaque cellule de chaque ligne, sans exception : une action du
        // dossier absente du document emis ferait douter de tout le document.
        assertThat(texte).contains("CONTAINMENT").contains("Isoler le lot")
                .contains("Ada Lovelace").contains("14/09/2026").contains("DONE");
        assertThat(texte).contains("PREVENTIVE").contains("Ajouter le controle")
                .contains("Alan Turing").contains("30/10/2026").contains("TODO");
    }

    /**
     * UN RAPPORT EMIS AVANT LES TABLEAUX se relit exactement comme il a ete
     * scelle. Son instantane ne porte pas de tableau -- {@code null} a la
     * relecture --, et son empreinte doit continuer de tomber juste : c'est
     * elle qui fait du document une preuve.
     */
    @Test
    void un_rapport_scelle_avant_les_tableaux_rend_les_memes_octets_qu_avant()
            throws IOException {
        List<EightDSnapshot.Section> sansTableau = new ArrayList<>();
        List<EightDSnapshot.Section> tableauxVides = new ArrayList<>();
        for (EightDDiscipline discipline : EightDDiscipline.values()) {
            List<String> lignes = List.of("Contenu de " + discipline.code());
            sansTableau.add(new EightDSnapshot.Section(
                    discipline.code(), discipline.titre(), true, "Source", lignes));
            tableauxVides.add(new EightDSnapshot.Section(
                    discipline.code(), discipline.titre(), true, "Source", lignes,
                    List.of(), null));
        }

        assertThat(renderer.render(snapshotDe(tableauxVides), URL))
                .isEqualTo(renderer.render(snapshotDe(sansTableau), URL));
    }

    /**
     * UNE LIGNE NE SE COUPE PAS, ET L'EN-TETE SE REPETE.
     *
     * <p>Une cellule dont la premiere moitie est en bas d'une page et la
     * seconde en haut de la suivante ne se lit plus. Pire : une colonne de
     * valeurs qui reprend sans son intitule se lit DE TRAVERS -- on prend une
     * echeance pour une date de cloture.
     *
     * <p>On fait varier la longueur du contenu qui precede, pour que la
     * coupure tombe a un endroit different a chaque tour.
     */
    @Test
    void un_tableau_qui_traverse_une_page_repete_son_en_tete() throws IOException {
        List<EightDSnapshot.Row> rangs = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            rangs.add(new EightDSnapshot.Row(List.of("CORRECTIVE",
                    "Action numero " + i + " : un intitule assez long pour se replier",
                    "Ada Lovelace", "14/09/2026", "TODO", "-")));
        }
        EightDSnapshot.Table longue = new EightDSnapshot.Table(
                "Dossier A", COLONNES, rangs);

        byte[] pdf = renderer.render(avecTableau(longue), URL);

        assertThat(compteDesOccurrences(texteDe(pdf), "DUE DATE"))
                .as("l'en-tete se repete sur chaque page que le tableau occupe")
                .isGreaterThan(1);
        // Et aucune ligne n'a disparu dans la coupure.
        String texte = texteDe(pdf);
        for (int i = 1; i <= 40; i++) {
            assertThat(texte).contains("Action numero " + i + " :");
        }
    }

    /** Deux rendus du meme instantane tabule donnent les memes octets. */
    @Test
    void deux_rendus_d_un_tableau_donnent_les_memes_octets() throws IOException {
        EightDSnapshot snapshot = avecTableau(tableauDeDemo());

        assertThat(renderer.render(snapshot, URL)).isEqualTo(renderer.render(snapshot, URL));
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


    /** Le numero de la page (1-based) ou ce texte apparait ; -1 s'il est absent. */
    private int pageQuiPorte(byte[] pdf, String recherche) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                PDFTextStripper s = new PDFTextStripper();
                s.setStartPage(p);
                s.setEndPage(p);
                if (s.getText(doc).contains(recherche)) {
                    return p;
                }
            }
        }
        return -1;
    }

    /**
     * Les textes qui passent SOUS le filet du pied d'integrite, le pied lui-meme
     * excepte. Vide, donc, quand rien ne se chevauche.
     */
    private List<String> contenuSousLeFilet(byte[] pdf) throws IOException {
        // Les memes valeurs que le rendu : marge 48, bandeau du pied 150.
        float filet = PDRectangle.A4.getHeight() - (48f + 150f);
        List<String> fautifs = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            int pageDuPied = pageQuiPorte(pdf, "Integrity & verification");
            PDFTextStripper releve = new PDFTextStripper() {
                @Override
                protected void writeString(String texte, List<TextPosition> positions) {
                    if (getCurrentPageNo() != pageDuPied) {
                        return;
                    }
                    for (TextPosition p : positions) {
                        // getYDirAdj compte depuis le HAUT de la page.
                        if (p.getYDirAdj() > filet && !duPied(texte)) {
                            fautifs.add(texte.trim());
                            return;
                        }
                    }
                }
            };
            releve.getText(doc);
        }
        return fautifs;
    }

    /** Ce que le pied d'integrite ecrit lui-meme, et qui a donc le droit d'y etre. */
    private static boolean duPied(String texte) {
        String s = texte.trim();
        return s.startsWith("Integrity & verification")
                || s.startsWith("Ce rapport est signe")
                || s.startsWith("Its content is frozen")
                || s.startsWith("Issued by:")
                || s.startsWith("Scannez le QR code")
                || s.startsWith("Tenant:");
    }

    /** Un instantane dont le D4 porte la figure, precede de {@code n} lignes de D3. */
    private EightDSnapshot avecRemplissage(EightDSnapshot.Figure figure, int n) {
        List<String> bourrage = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            bourrage.add("Ligne de remplissage " + i + " : assez longue pour que le repli de"
                    + " ligne entre en jeu et deplace la coupure de page.");
        }
        List<EightDSnapshot.Section> sections = new ArrayList<>();
        for (EightDDiscipline discipline : EightDDiscipline.values()) {
            boolean d4 = discipline == EightDDiscipline.D4;
            boolean d3 = discipline == EightDDiscipline.D3;
            sections.add(new EightDSnapshot.Section(
                    discipline.code(), discipline.titre(), true, "Source",
                    d4 ? List.of() : (d3 ? bourrage : List.of("Contenu de " + discipline.code())),
                    d4 ? List.of(figure) : List.of()));
        }
        return new EightDSnapshot("NC-2026-0007", "Fuite au presse-etoupe", "tenant-demo",
                "13/09/2026 10:00 UTC", "Ada Lovelace", false, sections);
    }


    /** Les colonnes du tableau d'actions, dans l'ordre de l'ecran. */
    private static final List<String> COLONNES =
            List.of("Kind", "Action", "Owner", "Due date", "Status", "Evidence");

    private EightDSnapshot.Table tableauDeDemo() {
        return new EightDSnapshot.Table("Dossier A - fuite presse-etoupe", COLONNES, List.of(
                new EightDSnapshot.Row(List.of("CONTAINMENT",
                        "Isoler le lot 2026-114 et bloquer l'expedition",
                        "Ada Lovelace", "14/09/2026", "DONE", "2")),
                new EightDSnapshot.Row(List.of("PREVENTIVE",
                        "Ajouter le controle d'etancheite a la gamme",
                        "Alan Turing", "30/10/2026", "TODO", "-"))));
    }

    /** Un instantane dont le D5 porte ce tableau, et rien d'autre. */
    private EightDSnapshot avecTableau(EightDSnapshot.Table tableau) {
        List<EightDSnapshot.Section> sections = new ArrayList<>();
        for (EightDDiscipline discipline : EightDDiscipline.values()) {
            boolean d5 = discipline == EightDDiscipline.D5;
            sections.add(new EightDSnapshot.Section(
                    discipline.code(), discipline.titre(), true, "Source",
                    d5 ? List.of() : List.of("Contenu de " + discipline.code()),
                    List.of(), d5 ? List.of(tableau) : List.of()));
        }
        return snapshotDe(sections);
    }

    private EightDSnapshot snapshotDe(List<EightDSnapshot.Section> sections) {
        return new EightDSnapshot("NC-2026-0008", "Fuite au presse-etoupe", "tenant-demo",
                "13/09/2026 10:00 UTC", "Ada Lovelace", false, sections);
    }

    private static int compteDesOccurrences(String texte, String motif) {
        int n = 0;
        int i = texte.indexOf(motif);
        while (i >= 0) {
            n++;
            i = texte.indexOf(motif, i + motif.length());
        }
        return n;
    }

    private String texteDe(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
