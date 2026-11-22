package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.application.DerogationPdfPort;
import com.openlab.qualitos.quality.derogation.domain.Analysis;
import com.openlab.qualitos.quality.derogation.domain.Derogation;
import com.openlab.qualitos.quality.derogation.domain.DerogationLanguage;
import com.openlab.qualitos.quality.derogation.domain.DerogationLine;
import com.openlab.qualitos.quality.derogation.domain.Identification;
import com.openlab.qualitos.quality.derogation.domain.ManagerApproval;
import com.openlab.qualitos.quality.derogation.domain.Nature;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Met en page la dérogation : les six sections de la fiche, dans leur ordre.
 *
 * <p>Le document est <b>conforme à l'interface d'édition</b> : ce que le client
 * lit est ce que l'initiateur a rempli, section par section. Ce n'est ni un
 * extrait ni une mise en page inventée pour l'occasion.
 *
 * <p>Il n'est <b>pas scellé</b> : ni signature cryptographique, ni ancrage. La
 * pièce opposable est l'exemplaire que le client renvoie signé de son côté, et le
 * pied de page le dit — sans cela le client croirait détenir une preuve.
 *
 * <p>Polices standard seulement, aucune dépendance à la machine. Une rubrique
 * vide est écrite « Non renseigné » plutôt que tue : le client doit voir ce qui
 * manque, pas le deviner.
 */
@Component
public class PdfBoxDerogationRenderAdapter implements DerogationPdfPort {

    private static final float[] INDIGO = {0.224f, 0.286f, 0.671f};
    private static final float[] INK = {0.102f, 0.102f, 0.180f};
    private static final float[] MUTED = {0.45f, 0.45f, 0.50f};
    private static final float[] PANEL = {0.96f, 0.97f, 0.99f};
    private static final float[] RULE = {0.85f, 0.86f, 0.90f};

    private static final float MARGIN = 48f;
    private static final float WIDTH = PDRectangle.A4.getWidth();
    private static final float HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_W = WIDTH - 2 * MARGIN;
    private static final float ROW = 15f;
    private static final float BOTTOM = MARGIN + 36f;

    /** Caractères d'un paragraphe sur la largeur utile, en 9 points. */
    private static final int WRAP = 104;
    /** Caractères d'une valeur de paire étiquette/valeur sur sa demi-largeur. */
    private static final int PAIR_MAX = 44;

    /** La signature est affichée à taille lisible, sans déformer son rapport. */
    private static final float SIGN_MAX_W = 180f;
    private static final float SIGN_MAX_H = 60f;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Paris"));

    private final PDType1Font regular =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    @Override
    public byte[] render(Derogation d, DerogationLanguage langue) {
        DerogationLabels l = new DerogationLabels(
                langue == null ? DerogationLanguage.FR : langue);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Page page = new Page(document);
            entete(page, d, l);
            section1(page, d, l);
            section2(page, d, l);
            section3(page, d, l);
            section4(page, d, l);
            section5(page, d, l, document);
            section6(page, d, l);
            page.fermer();
            piedsDePage(document, l);
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Rendu PDF de la dérogation impossible", e);
        }
    }

    // ---- les six sections ----------------------------------------------------

    private void entete(Page p, Derogation d, DerogationLabels l) throws IOException {
        p.bandeau(l.t("title"));
        p.paire(l.t("reference"), d.getReference(), l.t("status"), l.of(d.getStatus()), l);
        p.espace(6f);
    }

    private void section1(Page p, Derogation d, DerogationLabels l) throws IOException {
        p.titre(l.t("s1"));
        Identification i = d.getIdentification();
        p.paire(l.t("customer"), i.customerName(), l.t("contact"), i.customerQualityContact(), l);
        p.paire(l.t("order"), i.customerOrderNo(), l.t("part"), i.partReference(), l);
        p.paire(l.t("index"), i.partIndex(), l.t("batch"), i.batchNumber(), l);
        p.paire(l.t("quantity"), nombre(i.quantity()),
                l.t("requestDate"), i.requestDate() == null ? null : DATE.format(i.requestDate()),
                l);
        p.espace(6f);
    }

    private void section2(Page p, Derogation d, DerogationLabels l) throws IOException {
        p.titre(l.t("s2"));
        Nature n = d.getNature();
        p.paire(l.t("shipment"), l.of(n.shipmentStage()), l.t("origin"), l.of(n.deviationOrigin()),
                l);
        p.paire(l.t("occurrence"), l.of(n.occurrence()), l.t("duration"), l.of(n.durationKind()),
                l);
        if (n.durationDetail() != null) {
            p.bloc(l.t("durationDetail"), n.durationDetail(), l);
        }
        p.espace(6f);
    }

    private void section3(Page p, Derogation d, DerogationLabels l) throws IOException {
        p.titre(l.t("s3"));
        List<DerogationLine> lignes = d.getLines();
        if (lignes.isEmpty()) {
            p.sourdine(l.t("noLine"));
        } else {
            p.enTeteTableau(l.t("specification"), l.t("actual"), l.t("ncQty"));
            for (DerogationLine ligne : lignes) {
                p.ligneTableau(ligne.specification(), ligne.actualValue(),
                        nombre(ligne.nonConformingQty()), l);
            }
        }
        p.espace(4f);
        p.bloc(l.t("justification"), d.getJustification(), l);
        p.espace(6f);
    }

    private void section4(Page p, Derogation d, DerogationLabels l) throws IOException {
        p.titre(l.t("s4"));
        Analysis a = d.getAnalysis();
        p.bloc(l.t("rootCause"), a.rootCause(), l);
        p.bloc(l.t("containment"), a.containment(), l);
        p.bloc(l.t("corrective"), a.correctiveAction(), l);
        p.paire(l.t("capaRef"), a.capaReference(), l.t("attachments"), a.attachmentsNote(), l);
        p.espace(6f);
    }

    private void section5(Page p, Derogation d, DerogationLabels l, PDDocument document)
            throws IOException {
        p.titre(l.t("s5"));
        ManagerApproval a = d.getApproval();
        p.paire(l.t("manager"), a.managerAccountName(),
                l.t("signedBy"), a.signedByName() == null ? l.t("notSigned") : a.signedByName(),
                l);
        if (a.signedAt() != null) {
            p.paire(l.t("signedAt"), STAMP.format(a.signedAt()), null, null, l);
        }
        if (a.signaturePng() != null) {
            p.signature(document, a.signaturePng(), l.t("signature"));
        }
        p.espace(6f);
    }

    private void section6(Page p, Derogation d, DerogationLabels l) throws IOException {
        p.titre(l.t("s6"));
        p.paire(l.t("decision"), l.of(d.getAnswer().decision()),
                l.t("decisionDate"), d.getAnswer().decidedOn() == null
                        ? null : DATE.format(d.getAnswer().decidedOn()), l);
    }

    private void piedsDePage(PDDocument document, DerogationLabels l) throws IOException {
        int total = document.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage page = document.getPage(i);
            try (PDPageContentStream flux = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                flux.setNonStrokingColor(MUTED[0], MUTED[1], MUTED[2]);
                flux.beginText();
                flux.setFont(regular, 7.5f);
                flux.newLineAtOffset(MARGIN, MARGIN - 10f);
                flux.showText(reduire(l.t("footer")));
                flux.endText();
                String pagination = (i + 1) + " / " + total;
                flux.beginText();
                flux.setFont(regular, 7.5f);
                flux.newLineAtOffset(WIDTH - MARGIN - regular.getStringWidth(pagination)
                        / 1000f * 7.5f, MARGIN - 10f);
                flux.showText(pagination);
                flux.endText();
            }
        }
    }

    // ---- la page courante ----------------------------------------------------

    /**
     * La page en cours d'écriture, et son curseur.
     *
     * <p>Un objet, et non un {@code float} passé de méthode en méthode : le
     * curseur et le flux changent ensemble à chaque saut de page, et les séparer
     * laisserait écrire sur une page qu'on vient de quitter.
     */
    private final class Page {

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream flux;
        private float y;

        Page(PDDocument document) throws IOException {
            this.document = document;
            nouvelle();
        }

        private void nouvelle() throws IOException {
            if (flux != null) {
                flux.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            flux = new PDPageContentStream(document, page);
            y = HEIGHT - MARGIN;
        }

        void fermer() throws IOException {
            if (flux != null) {
                flux.close();
                flux = null;
            }
        }

        private void place(float hauteur) throws IOException {
            if (y - hauteur < BOTTOM) {
                nouvelle();
            }
        }

        void espace(float h) {
            y -= h;
        }

        void bandeau(String titre) throws IOException {
            flux.setNonStrokingColor(INDIGO[0], INDIGO[1], INDIGO[2]);
            flux.addRect(MARGIN, y - 26f, CONTENT_W, 26f);
            flux.fill();
            flux.setNonStrokingColor(1f, 1f, 1f);
            flux.beginText();
            flux.setFont(bold, 13f);
            flux.newLineAtOffset(MARGIN + 10f, y - 18f);
            flux.showText(reduire(titre));
            flux.endText();
            y -= 36f;
        }

        void titre(String titre) throws IOException {
            place(26f);
            flux.setNonStrokingColor(PANEL[0], PANEL[1], PANEL[2]);
            flux.addRect(MARGIN, y - 17f, CONTENT_W, 17f);
            flux.fill();
            flux.setNonStrokingColor(INDIGO[0], INDIGO[1], INDIGO[2]);
            flux.beginText();
            flux.setFont(bold, 9.5f);
            flux.newLineAtOffset(MARGIN + 6f, y - 12.5f);
            flux.showText(reduire(titre));
            flux.endText();
            y -= 24f;
        }

        /** Deux étiquettes et leurs valeurs, côte à côte. */
        void paire(String g, String vg, String dte, String vd, DerogationLabels l)
                throws IOException {
            place(ROW);
            float demi = CONTENT_W / 2f;
            cellule(MARGIN, g, vg, l);
            if (dte != null) {
                cellule(MARGIN + demi, dte, vd, l);
            }
            y -= ROW;
        }

        private void cellule(float x, String etiquette, String valeur, DerogationLabels l)
                throws IOException {
            flux.setNonStrokingColor(MUTED[0], MUTED[1], MUTED[2]);
            flux.beginText();
            flux.setFont(regular, 7.5f);
            flux.newLineAtOffset(x, y - 5f);
            flux.showText(reduire(etiquette));
            flux.endText();
            flux.setNonStrokingColor(INK[0], INK[1], INK[2]);
            flux.beginText();
            flux.setFont(regular, 9f);
            flux.newLineAtOffset(x, y - 14f);
            flux.showText(reduire(tronquer(valeur == null ? l.t("empty") : valeur, PAIR_MAX)));
            flux.endText();
        }

        /** Une étiquette et son paragraphe, replié sur la largeur utile. */
        void bloc(String etiquette, String texte, DerogationLabels l) throws IOException {
            place(ROW);
            flux.setNonStrokingColor(MUTED[0], MUTED[1], MUTED[2]);
            flux.beginText();
            flux.setFont(regular, 7.5f);
            flux.newLineAtOffset(MARGIN, y - 5f);
            flux.showText(reduire(etiquette));
            flux.endText();
            y -= 13f;
            for (String ligne : replier(texte == null ? l.t("empty") : texte, WRAP)) {
                place(11f);
                flux.setNonStrokingColor(INK[0], INK[1], INK[2]);
                flux.beginText();
                flux.setFont(regular, 9f);
                flux.newLineAtOffset(MARGIN, y - 4f);
                flux.showText(reduire(ligne));
                flux.endText();
                y -= 11f;
            }
            y -= 3f;
        }

        void sourdine(String texte) throws IOException {
            place(ROW);
            flux.setNonStrokingColor(MUTED[0], MUTED[1], MUTED[2]);
            flux.beginText();
            flux.setFont(regular, 9f);
            flux.newLineAtOffset(MARGIN, y - 8f);
            flux.showText(reduire(texte));
            flux.endText();
            y -= ROW;
        }

        void enTeteTableau(String a, String b, String c) throws IOException {
            place(ROW);
            flux.setNonStrokingColor(MUTED[0], MUTED[1], MUTED[2]);
            flux.beginText();
            flux.setFont(bold, 7.5f);
            flux.newLineAtOffset(MARGIN, y - 8f);
            flux.showText(reduire(a));
            flux.endText();
            flux.beginText();
            flux.setFont(bold, 7.5f);
            flux.newLineAtOffset(MARGIN + CONTENT_W * 0.40f, y - 8f);
            flux.showText(reduire(b));
            flux.endText();
            flux.beginText();
            flux.setFont(bold, 7.5f);
            flux.newLineAtOffset(MARGIN + CONTENT_W * 0.80f, y - 8f);
            flux.showText(reduire(c));
            flux.endText();
            y -= 13f;
            filet();
        }

        void ligneTableau(String a, String b, String c, DerogationLabels l) throws IOException {
            place(ROW);
            String tiret = "—";
            colonne(MARGIN, a == null ? tiret : a, 46);
            colonne(MARGIN + CONTENT_W * 0.40f, b == null ? tiret : b, 46);
            colonne(MARGIN + CONTENT_W * 0.80f, c == null ? tiret : c, 14);
            y -= ROW;
            filet();
        }

        private void colonne(float x, String valeur, int max) throws IOException {
            flux.setNonStrokingColor(INK[0], INK[1], INK[2]);
            flux.beginText();
            flux.setFont(regular, 9f);
            flux.newLineAtOffset(x, y - 10f);
            flux.showText(reduire(tronquer(valeur, max)));
            flux.endText();
        }

        private void filet() throws IOException {
            flux.setStrokingColor(RULE[0], RULE[1], RULE[2]);
            flux.setLineWidth(0.5f);
            flux.moveTo(MARGIN, y + 2f);
            flux.lineTo(WIDTH - MARGIN, y + 2f);
            flux.stroke();
        }

        /**
         * Le tracé de la signature, à son rapport d'origine.
         *
         * <p>Si l'image ne se décode pas ici, c'est l'émission qui échoue — mais
         * le tracé a déjà été vérifié au moment de signer, précisément pour que
         * ce cas n'arrive pas à l'envoi.
         */
        void signature(PDDocument document, byte[] png, String etiquette) throws IOException {
            place(SIGN_MAX_H + 18f);
            flux.setNonStrokingColor(MUTED[0], MUTED[1], MUTED[2]);
            flux.beginText();
            flux.setFont(regular, 7.5f);
            flux.newLineAtOffset(MARGIN, y - 5f);
            flux.showText(reduire(etiquette));
            flux.endText();
            y -= 12f;
            PDImageXObject image = PDImageXObject.createFromByteArray(document, png, "signature");
            float ratio = Math.min(SIGN_MAX_W / image.getWidth(),
                    SIGN_MAX_H / image.getHeight());
            float largeur = image.getWidth() * ratio;
            float hauteur = image.getHeight() * ratio;
            flux.drawImage(image, MARGIN, y - hauteur, largeur, hauteur);
            y -= hauteur + 6f;
        }
    }

    // ---- texte ---------------------------------------------------------------

    private static String nombre(java.math.BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }

    private static String tronquer(String s, int max) {
        if (s == null) {
            return "";
        }
        String net = s.replace('\n', ' ').replace('\r', ' ').strip();
        return net.length() <= max ? net : net.substring(0, max - 1) + "…";
    }

    /** Replie un paragraphe, en gardant les sauts de ligne saisis. */
    private static List<String> replier(String texte, int largeur) {
        List<String> lignes = new ArrayList<>();
        for (String paragraphe : texte.split("\r?\n", -1)) {
            if (paragraphe.isBlank()) {
                lignes.add("");
                continue;
            }
            StringBuilder courante = new StringBuilder();
            for (String mot : paragraphe.strip().split("\\s+")) {
                if (courante.length() + mot.length() + 1 > largeur && courante.length() > 0) {
                    lignes.add(courante.toString());
                    courante.setLength(0);
                }
                if (courante.length() > 0) {
                    courante.append(' ');
                }
                courante.append(mot);
            }
            if (courante.length() > 0) {
                lignes.add(courante.toString());
            }
        }
        return lignes;
    }

    /**
     * Les caractères que les polices standard savent écrire.
     *
     * <p>Calculé, et non écrit à la main : une table de deux cents entrées tenue
     * à la main se tromperait en silence, et le rendu échouerait sur un caractère
     * oublié — à l'émission d'un document qui part au client.
     */
    private static final BitSet ENCODABLE = encodableParLesPolices();

    private static BitSet encodableParLesPolices() {
        CharsetEncoder winAnsi = Charset.forName("windows-1252").newEncoder();
        BitSet jeu = new BitSet(Character.MAX_VALUE + 1);
        for (int c = ' '; c <= Character.MAX_VALUE; c++) {
            if (c != 0x7F && winAnsi.canEncode((char) c)) {
                jeu.set(c);
            }
        }
        return jeu;
    }

    /**
     * Les symboles techniques qui n'entrent pas dans le jeu WinAnsi, et leur
     * équivalent écrit.
     *
     * <p><b>C'est le point le plus important de cette classe.</b> Une dérogation
     * porte des cotes et des tolérances : « ≤ 5 µm », « Ø 12 ± 0,02 ». Remplacer
     * ces signes par un {@code ?} — ce que fait le rapport 8D, où ils ne figurent
     * pas — changerait une spécification sur un document qui engage l'entreprise
     * auprès de son client. « ? 5 ?m » ne dit plus rien, et personne ne s'en
     * apercevrait avant la réception.
     *
     * <p>On translittère donc, en clair, ce qui a un sens technique ; le reste
     * tombe sur {@code ?}, qui au moins se voit. Note : « ± », « ° », « µ » (le
     * micro U+00B5) et « × » sont <i>déjà</i> dans WinAnsi et passent intacts ;
     * ce sont leurs homographes Unicode — U+2264, U+2265, U+03BC, U+00D8 hors
     * jeu, U+2300 — qui ont besoin de cette table.
     */
    private static final Map<Character, String> TRANSLITTERATION = Map.ofEntries(
            Map.entry('≤', "<="),      // inférieur ou égal
            Map.entry('≥', ">="),      // supérieur ou égal
            Map.entry('≠', "!="),      // différent de
            Map.entry('≈', "~"),       // environ égal
            Map.entry('μ', "µ"),  // mu grec -> micro WinAnsi
            Map.entry('⌀', "dia."),    // symbole de diamètre
            Map.entry('∅', "dia."),    // ensemble vide, employé pour le diamètre
            Map.entry('○', "dia."),    // cercle, même usage
            Map.entry('→', "->"),      // flèche : nos libellés d'écart en portent
            Map.entry('⇒', "=>"),
            Map.entry('²', "2"),       // au carré, hors jeu dans certaines polices
            Map.entry('³', "3"),
            Map.entry('′', "'"),       // minute d'angle
            Map.entry('″', "\""),      // seconde d'angle
            Map.entry('−', "-"),       // signe moins typographique
            Map.entry('±', "+/-"));    // plus ou moins, si la police le refuse

    /**
     * Ramène le texte à ce que les polices savent encoder.
     *
     * <p>Le français passe tel quel : WinAnsi porte les accents, la cédille, la
     * ligature « œ », l'apostrophe typographique et le tiret cadratin.
     */
    static String reduire(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ENCODABLE.get(ch)) {
                b.append(ch);
                continue;
            }
            String equivalent = TRANSLITTERATION.get(ch);
            if (equivalent == null) {
                b.append('?');
                continue;
            }
            // L'equivalent lui-meme doit etre encodable : une table fausse ne
            // doit pas faire echouer l'emission.
            for (int j = 0; j < equivalent.length(); j++) {
                char e = equivalent.charAt(j);
                b.append(ENCODABLE.get(e) ? e : '?');
            }
        }
        return b.toString();
    }
}
