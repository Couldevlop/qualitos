package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import com.openlab.qualitos.quality.dmaic.resolution.application.ClaimReport;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionPorts;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Met en page le rapport d'action corrective client, dans l'ordre de la feuille
 * « Client Claim Rapport » du classeur : en-tête de la réclamation, puis les
 * cinq étapes D, M, A, I, C, chacune avec ce qu'en reprend le modèle client.
 *
 * <p>Une rubrique vide est écrite « Non renseigné » plutôt que tue : le client
 * doit voir ce qui manque, pas deviner.
 *
 * <p>Polices standard seulement (aucune dépendance à la machine) ; le texte est
 * ramené au jeu WinAnsi, qui garde les accents français.
 */
public class PdfBoxClaimReportRenderer implements ResolutionPorts.ClaimReportRenderer {

    private static final float[] INDIGO = {0.224f, 0.286f, 0.671f};
    private static final float[] INK = {0.102f, 0.102f, 0.180f};
    private static final float[] MUTED = {0.45f, 0.45f, 0.50f};
    private static final float[] PANEL = {0.96f, 0.97f, 0.99f};

    private static final float MARGIN = 48f;
    private static final float WIDTH = PDRectangle.A4.getWidth();
    private static final float HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_W = WIDTH - 2 * MARGIN;
    private static final int WRAP = 100;
    /** Caractères d'une valeur d'en-tête sur sa demi-largeur. */
    private static final int PAIR_MAX = 30;
    /** Caractères d'une valeur d'en-tête sur toute la largeur. */
    private static final int WIDE_MAX = 80;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");


    static final String EMPTY = "Non renseigné";

    @Override
    public byte[] render(ClaimReport r) {
        try (PDDocument doc = new PDDocument()) {
            Writer w = new Writer(doc);
            doc.getDocumentInformation().setTitle(sanitize("Rapport d'action corrective client "
                    + (r.claimNumber() == null ? "" : r.claimNumber())));
            doc.getDocumentInformation().setProducer("QualitOS");

            w.text(w.bold, 18, INDIGO, "Rapport d'action corrective client DMAIC");
            w.y -= 8;
            w.rule();
            w.y -= 18;
            header(w, r);

            w.step("D : Définir — étape 1");
            answers(w, r.define());

            w.step("M : Mesurer — étape 2");
            w.label("Synthèse des mesures");
            w.body(r.measureSummary());

            w.step("A : Analyser — étape 3");
            w.label("Plan d'action temporaire pour contenir le problème");
            w.body(r.containmentPlan());
            w.label("Mesures conservatoires");
            w.body(r.containmentMeasures());
            w.label("Diagramme d'Ishikawa");
            if (r.ishikawaCauses().isEmpty()) {
                w.body(null);
            }
            for (ClaimReport.Answer c : r.ishikawaCauses()) {
                w.paragraph(c.question() + " : " + c.answer(), INK, w.regular, 9.5f);
            }
            w.y -= 6;
            w.label("5 pourquoi");
            if (r.whys().isEmpty()) {
                w.body(null);
            }
            for (ClaimReport.Why why : r.whys()) {
                w.sub(why.branch() + (why.rootCause() ? " — cause racine" : ""));
                w.paragraph("Constat : " + orEmpty(why.cause()), INK, w.regular, 9.5f);
                for (int i = 0; i < why.whys().size(); i++) {
                    w.paragraph("Pourquoi " + (i + 1) + " : " + why.whys().get(i), INK, w.regular, 9.5f);
                }
                if (why.action() != null && !why.action().isBlank()) {
                    w.paragraph("Action : " + why.action(), INK, w.regular, 9.5f);
                }
                w.y -= 6;
            }
            w.label("Cause racine");
            w.body(r.rootCause());

            w.step("I : Améliorer — étape 4");
            w.label("Meilleures solutions sélectionnées");
            w.body(r.bestSolutions());
            w.label("Plan d'actions");
            if (r.actions().isEmpty()) {
                w.body(null);
            }
            int n = 1;
            for (ClaimReport.Action a : r.actions()) {
                w.paragraph(n++ + ". " + orEmpty(a.action()), INK, w.bold, 9.5f);
                w.paragraph("Responsable : " + orEmpty(a.responsible()) + "  ·  Statut : " + orEmpty(a.status())
                        + "  ·  Prévue le : " + orEmpty(date(a.plannedOn())) + "  ·  Réalisée le : "
                        + orEmpty(date(a.doneOn())) + "  ·  Avancement : " + a.percent() + " %",
                        MUTED, w.regular, 8.5f);
                w.y -= 4;
            }

            w.step("C : Contrôler — étape 5");
            answers(w, List.of(new ClaimReport.Answer(
                    "Le plan de surveillance élimine-t-il ou minimise-t-il la récurrence, et comment est-ce mesuré ?",
                    r.surveillance())));

            w.flux.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("claim report rendering failed", e);
        }
    }

    /** Une question du modèle, puis sa réponse (ou « Non renseigné »). */
    private static void answers(Writer w, List<ClaimReport.Answer> answers) throws IOException {
        if (answers.isEmpty()) {
            w.body(null);
        }
        for (ClaimReport.Answer a : answers) {
            w.label(a.question());
            w.body(a.answer());
        }
    }

    private static String orEmpty(String s) {
        return s == null || s.isBlank() ? EMPTY : s;
    }

    private static void header(Writer w, ClaimReport r) throws IOException {
        String[][] rows = {
                {"Client", r.clientName(), "Réclamation n°", r.claimNumber()},
                {"Projet", r.projectName(), "Date de création", date(r.openedOn())},
                {"Propriétaire", r.ownerName(), "Date de fermeture", date(r.closedOn())},
                {"Sévérité", r.severity(), null, null}};
        // Le type de défaillance a sa ligne entière : ses libellés sont longs.
        float height = (rows.length + 1) * 18f + 12f;
        w.panel(height);
        float y = w.y - 18;
        for (String[] row : rows) {
            w.pair(MARGIN + 12, y, row[0], row[1]);
            if (row[2] != null) {
                w.pair(MARGIN + CONTENT_W / 2 + 6, y, row[2], row[3]);
            }
            y -= 18;
        }
        w.wide(MARGIN + 12, y, "Type de défaillance", r.failureType());
        w.y -= height + 16;
    }

    private static String date(LocalDate d) {
        return d == null ? null : DATE.format(d);
    }

    /** Le curseur d'écriture : une page ouverte et la hauteur où l'on écrit. */
    private static final class Writer {
        private final PDDocument doc;
        private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPageContentStream flux;
        private float y;

        Writer(PDDocument doc) throws IOException {
            this.doc = doc;
            newPage();
        }

        void newPage() throws IOException {
            if (flux != null) {
                flux.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            flux = new PDPageContentStream(doc, page);
            y = HEIGHT - MARGIN;
        }

        void room(float needed) throws IOException {
            if (y - needed < MARGIN) {
                newPage();
            }
        }

        void step(String title) throws IOException {
            room(48);
            y -= 10;
            text(bold, 13, INDIGO, title);
            y -= 6;
            rule();
            y -= 16;
        }

        void label(String s) throws IOException {
            room(28);
            text(bold, 9.5f, INK, s);
            y -= 14;
        }

        void sub(String s) throws IOException {
            room(24);
            text(bold, 9, MUTED, s);
            y -= 13;
        }

        void body(String s) throws IOException {
            if (s == null || s.isBlank()) {
                paragraph(EMPTY, MUTED, regular, 9.5f);
            } else {
                paragraph(s, INK, regular, 9.5f);
            }
            y -= 6;
        }

        void paragraph(String s, float[] color, PDType1Font font, float size) throws IOException {
            for (String line : s.split("\\R")) {
                for (String part : wrap(line)) {
                    room(14);
                    at(font, size, color, MARGIN + 10, y, part);
                    y -= 12.5f;
                }
            }
        }

        void text(PDType1Font font, float size, float[] color, String s) throws IOException {
            at(font, size, color, MARGIN, y, s);
        }

        /** Une clé et sa valeur sur une demi-largeur ; une valeur trop longue est abrégée. */
        void pair(float x, float atY, String key, String value) throws IOException {
            field(x, atY, key, value, PAIR_MAX);
        }

        void wide(float x, float atY, String key, String value) throws IOException {
            field(x, atY, key, value, WIDE_MAX);
        }

        private void field(float x, float atY, String key, String value, int max) throws IOException {
            at(bold, 8.5f, MUTED, x, atY, key);
            String v = value == null || value.isBlank() ? "—" : value;
            at(regular, 9.5f, INK, x + 96, atY, v.length() > max ? v.substring(0, max - 1) + "…" : v);
        }

        void panel(float height) throws IOException {
            room(height + 10);
            flux.setNonStrokingColor(PANEL[0], PANEL[1], PANEL[2]);
            flux.addRect(MARGIN, y - height, CONTENT_W, height);
            flux.fill();
        }

        void rule() throws IOException {
            flux.setStrokingColor(INDIGO[0], INDIGO[1], INDIGO[2]);
            flux.setLineWidth(1.2f);
            flux.moveTo(MARGIN, y);
            flux.lineTo(WIDTH - MARGIN, y);
            flux.stroke();
        }

        private void at(PDType1Font font, float size, float[] color, float x, float atY, String s)
                throws IOException {
            flux.beginText();
            flux.setFont(font, size);
            flux.setNonStrokingColor(color[0], color[1], color[2]);
            flux.newLineAtOffset(x, atY);
            flux.showText(sanitize(s));
            flux.endText();
        }
    }

    static List<String> wrap(String line) {
        List<String> parts = new ArrayList<>();
        String rest = line.strip();
        if (rest.isEmpty()) {
            parts.add("");
            return parts;
        }
        while (rest.length() > WRAP) {
            int cut = rest.lastIndexOf(' ', WRAP);
            if (cut <= 0) {
                cut = WRAP;
            }
            parts.add(rest.substring(0, cut).stripTrailing());
            rest = rest.substring(cut).stripLeading();
        }
        parts.add(rest);
        return parts;
    }

    /**
     * Ramène au jeu WinAnsi des polices standard : le Latin-1 imprimable passe
     * tel quel (accents compris), le reste est dégradé plutôt que de faire
     * échouer le rendu.
     */
    static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 32 && ch < 127) || (ch >= 160 && ch <= 255)) {
                b.append(ch);
            } else {
                switch (ch) {
                    case '’', '‘' -> b.append('\'');
                    case '“', '”' -> b.append('"');
                    case '–', '—' -> b.append(ch);
                    case '…' -> b.append(ch);
                    case 'œ' -> b.append("oe");
                    case 'Œ' -> b.append("OE");
                    case '\t' -> b.append(' ');
                    default -> b.append('?');
                }
            }
        }
        return b.toString();
    }
}
