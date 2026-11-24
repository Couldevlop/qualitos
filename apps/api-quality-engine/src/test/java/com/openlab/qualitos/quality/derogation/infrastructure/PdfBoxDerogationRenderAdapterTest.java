package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.domain.Analysis;
import com.openlab.qualitos.quality.derogation.domain.Derogation;
import com.openlab.qualitos.quality.derogation.domain.DerogationLanguage;
import com.openlab.qualitos.quality.derogation.domain.DerogationLine;
import com.openlab.qualitos.quality.derogation.domain.DeviationOrigin;
import com.openlab.qualitos.quality.derogation.domain.DurationKind;
import com.openlab.qualitos.quality.derogation.domain.Identification;
import com.openlab.qualitos.quality.derogation.domain.Nature;
import com.openlab.qualitos.quality.derogation.domain.Occurrence;
import com.openlab.qualitos.quality.derogation.domain.ShipmentStage;
import com.openlab.qualitos.quality.derogation.domain.SignatureTrace;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le document qui part au client, relu avec un extracteur de texte.
 *
 * <p>Deux bancs portent l'essentiel :
 * <ul>
 *   <li>une valeur de cinq cents caractères doit se retrouver <b>entière</b>.
 *       Helvetica ne replie rien d'elle-même : sans repliement explicite, le
 *       client lirait une spécification amputée ;</li>
 *   <li>les symboles de tolérance (Ø, ±, ≤, µ, ²) ne sont pas dans WinAnsi.
 *       Les remplacer par « ? » changerait la cote : « Ø12 ±0,02 » deviendrait
 *       « ?12 ?0,02 ». Ils sont donc translittérés.</li>
 * </ul>
 */
class PdfBoxDerogationRenderAdapterTest {

    private static final UUID CLIENT = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEMANDEUR = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MANAGER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant T0 = Instant.parse("2026-11-22T10:00:00Z");

    private final PdfBoxDerogationRenderAdapter rendu = new PdfBoxDerogationRenderAdapter();

    @Test
    void leDocumentEstUnPdfLisibleQuiPorteLesSixSections() throws IOException {
        String texte = texteDe(rendu.render(dossierComplet(), DerogationLanguage.FR));
        assertThat(texte)
                .contains("Demande de dérogation client")
                .contains("DER-2026-0001")
                .contains("ACME Industrie")
                .contains("P-4420 Bride acier")
                .contains("Nina Costa")
                .contains("8D-2026-0042");
    }

    @Test
    void uneValeurDeCinqCentsCaracteresSeRetrouveENTIERE() throws IOException {
        // Le defaut qu'on ne verrait qu'au recu du client : une tolerance
        // tronquee a quarante-quatre caracteres.
        String longue = "Tolerance geometrique de localisation applicable a l'alesage "
                + "principal apres traitement thermique, mesuree sur machine a mesurer "
                + "tridimensionnelle avec palpeur de un millimetre, selon le plan de "
                + "definition indice C et la gamme de controle GC-4420-07, en tenant "
                + "compte du referentiel primaire A sur la face d'appui, du secondaire "
                + "B sur l'axe de la bride et du tertiaire C sur le percage de "
                + "positionnement angulaire.";
        assertThat(longue).hasSizeGreaterThan(400);

        Derogation d = dossierComplet();
        d.reopen(DEMANDEUR, T0);
        d.edit(DEMANDEUR, identification(), Nature.parDefaut(),
                List.of(new DerogationLine(1, longue, "0,08 mm", new BigDecimal("4"))),
                longue, analyse(), T0);

        String texte = sansRetours(texteDe(rendu.render(d, DerogationLanguage.FR)));
        assertThat(texte).contains(sansRetours(longue));
    }

    @Test
    void lesSymbolesDeToleranceNeDeviennentPasDesPointsDInterrogation() throws IOException {
        Derogation d = dossierComplet();
        d.reopen(DEMANDEUR, T0);
        d.edit(DEMANDEUR, identification(), Nature.parDefaut(),
                List.of(new DerogationLine(1, "Ø12 ±0,02 / Ra ≤ 5 µm",
                        "Ø12,07 Ra 6,2 µm ≈ ≥ seuil", new BigDecimal("4"))),
                "Rugosite ≠ specifiee, surface 12 mm²", analyse(), T0);

        String texte = sansRetours(texteDe(rendu.render(d, DerogationLanguage.FR)));
        // Ø, ± et ² SONT dans WinAnsi : ils passent tels quels, et c'est tant
        // mieux -- une cote reste ecrite comme sur le plan.
        assertThat(texte).contains("Ø12 ±0,02").contains("12 mm²");
        // Seuls les symboles hors WinAnsi sont translitteres, parce que PDFBox
        // leve dessus et qu'un « ? » changerait la cote.
        assertThat(texte).contains("<= 5 µm").contains(">= seuil").contains("~");
        assertThat(texte).contains("!= specifiee");
        // Et surtout : aucun caractere n'est tombe en « ? ».
        assertThat(texte).doesNotContain("?");
    }

    @Test
    void unCaractereVraimentInconnuNeFaitPasEchouerLeRendu() throws IOException {
        // Un ideogramme colle depuis un courriel ne doit pas empecher d'envoyer
        // la demande : il se remplace, le document part.
        Derogation d = dossierComplet();
        d.reopen(DEMANDEUR, T0);
        d.edit(DEMANDEUR, identification(), Nature.parDefaut(),
                List.of(new DerogationLine(1, "Cote 中文 12,4", "12,9", null)),
                "ok", analyse(), T0);
        assertThat(texteDe(rendu.render(d, DerogationLanguage.FR))).contains("Cote");
    }

    @Test
    void laVersionAnglaiseEstEnAnglais() throws IOException {
        String fr = texteDe(rendu.render(dossierComplet(), DerogationLanguage.FR));
        String en = texteDe(rendu.render(dossierComplet(), DerogationLanguage.EN));
        assertThat(en).isNotEqualTo(fr);
        assertThat(sansRetours(en)).containsIgnoringCase("concession");
    }

    @Test
    void lePiedDePageNAvanceQueLaPagination() throws IOException {
        // Rien d'invente sur un document que lit un tiers : pas de mention de
        // scellement, pas de clause. Seulement la pagination.
        String texte = sansRetours(texteDe(rendu.render(dossierComplet(),
                DerogationLanguage.FR)));
        assertThat(texte).containsPattern("1\\s*/\\s*\\d+");
        assertThat(texte).doesNotContainIgnoringCase("scell");
    }

    @Test
    void unBrouillonVideSeRendSansEchouer() throws IOException {
        // Le document s'imprime a tout moment, y compris sur une fiche a peine
        // ouverte : sinon on ne pourrait pas relire avant de completer.
        Derogation nu = Derogation.create(CLIENT, "DER-2026-0002", DEMANDEUR, "Paul Girard",
                new Identification(null, null, null, null, null, null, null, null), T0);
        assertThat(texteDe(rendu.render(nu, DerogationLanguage.FR))).contains("DER-2026-0002");
    }

    @Test
    void laSignatureEstDessineeDansLeDocument() throws IOException {
        byte[] avec = rendu.render(dossierComplet(), DerogationLanguage.FR);
        Derogation sans = dossierComplet();
        sans.reopen(DEMANDEUR, T0);
        byte[] rouvert = rendu.render(sans, DerogationLanguage.FR);
        // L'image pese : un document signe est plus lourd que le meme non signe.
        assertThat(avec.length).isGreaterThan(rouvert.length);
    }

    @Test
    void beaucoupDeLignesDebordentSurUneSecondePage() throws IOException {
        Derogation d = dossierComplet();
        d.reopen(DEMANDEUR, T0);
        List<DerogationLine> lignes = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            lignes.add(new DerogationLine(i, "Cote numero " + i + " du plan indice C",
                    "mesure " + i + ",5 mm", new BigDecimal(i)));
        }
        d.edit(DEMANDEUR, identification(), Nature.parDefaut(), lignes, "ok", analyse(), T0);
        byte[] document = rendu.render(d, DerogationLanguage.FR);
        try (PDDocument doc = Loader.loadPDF(document)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
        String texte = sansRetours(texteDe(document));
        assertThat(texte).contains("Cote numero 1 du plan").contains("Cote numero 40 du plan");
    }

    @Test
    void lesQuatreChoixDeLaSectionDeuxFigurentEnClair() throws IOException {
        Derogation d = dossierComplet();
        d.reopen(DEMANDEUR, T0);
        d.edit(DEMANDEUR, identification(),
                new Nature(ShipmentStage.ALREADY_SHIPPED, DeviationOrigin.PROCESS,
                        Occurrence.REPEAT, DurationKind.PERMANENT, "jusqu'au plan indice D"),
                List.of(), "ok", analyse(), T0);
        String texte = sansRetours(texteDe(rendu.render(d, DerogationLanguage.FR)));
        assertThat(texte).contains("Pièces déjà expédiées")
                .contains("Lié au process")
                .contains("Répétition")
                .contains("Permanente");
    }

    // ---- fabriques -----------------------------------------------------------

    private static Derogation dossierComplet() {
        Derogation d = Derogation.create(CLIENT, "DER-2026-0001", DEMANDEUR, "Paul Girard",
                identification(), T0);
        d.edit(DEMANDEUR, identification(), Nature.parDefaut(),
                List.of(new DerogationLine(1, "Cote 12,4 +/- 0,1", "12,9", new BigDecimal("4"))),
                "Pieces fonctionnelles, hors zone d'ajustement", analyse(), T0);
        d.requestApproval(DEMANDEUR, MANAGER, "Nina Costa", T0);
        d.approve(MANAGER, "Nina Costa", SignatureTrace.of(png()), T0);
        return d;
    }

    private static Identification identification() {
        return new Identification("ACME Industrie", "qualite@acme.fr", "CMD-7781",
                "P-4420 Bride acier", "C", "LOT-2026-11", new BigDecimal("120"),
                LocalDate.of(2026, 11, 22));
    }

    private static Analysis analyse() {
        return new Analysis("Usure de l'outil de reprise", "Lot isole et trie a 100 %",
                "Remplacement de l'outil et controle en cours de serie",
                "8D-2026-0042, prevu le 15/12/2026", "Rapport de mesures, 3 photos");
    }

    @Test
    void deuxLignesVoisinesNeSeChevauchentJAMAIS() throws IOException {
        // Le defaut que ce banc verrouille : la hauteur de ligne valait 15
        // points pour une etiquette de 7,5 posee a -5 et une valeur de 9 posee
        // a -14. L etiquette de la ligne SUIVANTE montait alors a 14,6 sous
        // l origine quand la valeur de la precedente descendait a 15,9 : les
        // deux se superposaient de 1,3 point. Sur un document qui part au
        // client, l identification, la nature et les ecarts se lisaient comme
        // un bloc compact ou rien ne se detachait.
        //
        // On ne mesure pas une apparence, on mesure des lignes de base : c est
        // la seule grandeur qui dit si deux textes peuvent se toucher.
        List<Float> bases = lignesDeBase(rendu.render(dossierComplet(), DerogationLanguage.FR));

        assertThat(bases).hasSizeGreaterThan(20);
        for (int i = 1; i < bases.size(); i++) {
            float ecart = bases.get(i) - bases.get(i - 1);
            assertThat(ecart)
                    .as("deux lignes de base a %.1f et %.1f, soit %.1f point d ecart"
                            + " pour un texte de 9 points", bases.get(i - 1), bases.get(i), ecart)
                    .isGreaterThanOrEqualTo(10f);
        }
    }

    /**
     * Les lignes de base distinctes de la premiere page, de haut en bas.
     *
     * <p>Les deux colonnes d une paire partagent la MEME ligne de base : on
     * regroupe donc par ordonnee avant de comparer, sans quoi on mesurerait
     * l ecart entre un libelle de gauche et son voisin de droite, qui est nul
     * et parfaitement normal.
     */
    private static List<Float> lignesDeBase(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            java.util.TreeSet<Float> ordonnees = new java.util.TreeSet<>();
            PDFTextStripper releve = new PDFTextStripper() {
                @Override
                protected void writeString(String texte, List<TextPosition> positions) {
                    for (TextPosition position : positions) {
                        ordonnees.add(Math.round(position.getYDirAdj() * 10f) / 10f);
                    }
                }
            };
            releve.setStartPage(1);
            releve.setEndPage(1);
            releve.getText(document);
            return new ArrayList<>(ordonnees);
        }
    }

    private static String texteDe(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    /** Le repliement insère des retours : on compare le texte continu. */
    private static String sansRetours(String texte) {
        return texte.replaceAll("\\s+", " ").strip();
    }

    private static byte[] png() {
        BufferedImage image = new BufferedImage(160, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 160, 50);
        g.setColor(Color.BLACK);
        g.drawLine(6, 40, 150, 10);
        g.drawLine(20, 12, 90, 44);
        g.dispose();
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
