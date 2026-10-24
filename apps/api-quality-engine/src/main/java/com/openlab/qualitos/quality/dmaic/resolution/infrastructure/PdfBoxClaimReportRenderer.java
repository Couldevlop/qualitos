package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import com.openlab.qualitos.quality.dmaic.resolution.application.ClaimReport;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionPorts;
import com.openlab.qualitos.quality.dmaic.resolution.domain.DefineTopic;
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
import java.util.Map;

/**
 * Met en page le rapport d'action corrective client, dans l'ordre de la feuille
 * « Client Claim Rapport » du classeur : en-tête de la réclamation, puis les
 * cinq étapes D, M, A, I, C.
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

    private static final Map<DefineTopic, String> TOPICS = Map.of(
            DefineTopic.WHAT, "Quoi",
            DefineTopic.WHO, "Qui",
            DefineTopic.WHY, "Pourquoi",
            DefineTopic.WHERE, "Où",
            DefineTopic.WHEN, "Quand",
            DefineTopic.HOW_MANY, "Combien");

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
            w.label("Symptôme(s) de défaut ou de tendance tels que reportés par le client");
            w.body(r.customerSymptoms());
            w.label("Définition du problème");
            for (ClaimReport.Topic t : r.definition()) {
                w.sub(TOPICS.get(t.topic()));
                if (t.answers().isEmpty()) {
                    w.body(null);
                }
                for (ClaimReport.Answer a : t.answers()) {
                    w.paragraph(a.question(), MUTED, w.regular, 8.5f);
                    w.body(a.answer());
                }
            }
            w.label("Énoncé du problème");
            w.body(r.problemStatement());

            w.step("M : Mesurer — étape 2");
            w.label("Synthèse des informations, données et faits relatés");
            w.body(r.measureSummary());

            w.step("A : Analyser — mesures conservatoires — étape 3");
            w.label("Actions immédiates mises en place pour préserver le client du problème");
            w.body(r.containmentMeasures());
            w.label("Causes racines");
            w.body(r.rootCause());

            w.step("I : Améliorer — étape 4");
            w.label("Solutions retenues et mises en place");
            w.body(r.retainedSolutions());

            w.step("C : Contrôler — étape 5");
            w.label("Vérification des solutions et plan de surveillance");
            w.body(r.surveillancePlan());

            w.flux.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("claim report rendering failed", e);
        }
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
