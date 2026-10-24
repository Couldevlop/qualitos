package com.openlab.qualitos.quality.supplier;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Met en page la fiche d'une NC fournisseur : l'identification de la pièce, le
 * suivi du 8D, la description, la résolution et la liste des pièces jointes.
 *
 * <p>Une rubrique vide est écrite « Non renseigné » plutôt que tue : le
 * fournisseur doit voir ce qui manque, pas deviner. Polices standard seulement
 * (aucune dépendance à la machine) ; le texte est ramené au jeu WinAnsi, qui
 * garde les accents français.
 */
@Component
public class SupplierNcReportRenderer {

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
    private static final float ROW = 18f;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Paris"));

    static final String EMPTY = "Non renseigné";

    private static final Map<NonConformitySeverity, String> SEVERITIES = Map.of(
            NonConformitySeverity.MINOR, "Mineure",
            NonConformitySeverity.MAJOR, "Majeure",
            NonConformitySeverity.CRITICAL, "Critique");

    private static final Map<NonConformityStatus, String> STATUSES = Map.of(
            NonConformityStatus.OPEN, "Ouverte",
            NonConformityStatus.IN_REVIEW, "En analyse",
            NonConformityStatus.RESOLVED, "Résolue",
            NonConformityStatus.REJECTED, "Rejetée");

    private static final Map<SupplierDocumentCategory, String> CATEGORIES = Map.of(
            SupplierDocumentCategory.EIGHT_D, "8D fournisseur",
            SupplierDocumentCategory.CAPA_EVIDENCE, "Preuve CAPA",
            SupplierDocumentCategory.CERTIFICATE, "Certificat",
            SupplierDocumentCategory.OTHER, "Autre");

    public byte[] render(SupplierNcReport r) {
        try (PDDocument doc = new PDDocument()) {
            Writer w = new Writer(doc);
            doc.getDocumentInformation().setTitle(sanitize("Fiche de non-conformité fournisseur "
                    + nonNull(r.supplierCode()) + " " + nonNull(r.partNumber())).strip());
            doc.getDocumentInformation().setProducer("QualitOS");

            w.text(w.bold, 18, INDIGO, "Fiche de non-conformité fournisseur");
            w.y -= 8;
            w.rule();
            w.y -= 18;
            header(w, r);

            w.step("Définition de la non-conformité");
            w.body(r.description());

            w.step("Résolution");
            w.body(r.resolution());

            w.step("Pièces jointes (" + r.attachments().size() + ")");
            if (r.attachments().isEmpty()) {
                w.body(null);
            }
            for (SupplierNcReport.Attachment a : r.attachments()) {
                w.paragraph("• " + CATEGORIES.get(a.category()) + " — " + nonBlank(a.filename(), "sans nom")
                        + " (" + size(a.sizeBytes()) + ", " + STAMP.format(a.addedAt()) + ")", INK, w.regular, 9.5f);
            }

            w.y -= 18;
            w.paragraph("Édité le " + STAMP.format(r.editedAt()) + " par QualitOS.", MUTED, w.regular, 8f);

            w.flux.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("supplier non-conformity report rendering failed", e);
        }
    }

    private static void header(Writer w, SupplierNcReport r) throws IOException {
        String supplier = r.supplierCode() == null ? r.supplierName()
                : r.supplierCode() + (r.supplierName() == null ? "" : " — " + r.supplierName());
        String[][] rows = {
                {"Fournisseur", supplier, "Statut", STATUSES.get(r.status())},
                {"Référence pièce", r.partNumber(), "Gravité", SEVERITIES.get(r.severity())},
                {"Nombre de pièces", r.partQuantity() == null ? null : String.valueOf(r.partQuantity()),
                        "Lot", r.lotReference()},
                {"Détectée le", date(r.detectedOn()), "Résolue le", date(r.resolvedOn())},
                {"8D demandé", yesNo(r.eightDRequested()), "8D reçu", yesNo(r.eightDReceived())}};
        float height = rows.length * ROW + 12f;
        w.panel(height);
        float y = w.y - ROW;
        for (String[] row : rows) {
            w.pair(MARGIN + 12, y, row[0], row[1]);
            w.pair(MARGIN + CONTENT_W / 2 + 6, y, row[2], row[3]);
            y -= ROW;
        }
        w.y -= height + 16;
    }

    private static String yesNo(boolean b) {
        return b ? "Oui" : "Non";
    }

    private static String date(LocalDate d) {
        return d == null ? null : DATE.format(d);
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    private static String nonBlank(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s;
    }

    /** Taille lisible : 1,2 Mo plutôt que 1258291. */
    static String size(long bytes) {
        if (bytes >= 1024L * 1024) {
            return String.format(Locale.FRANCE, "%.1f Mo", bytes / (1024d * 1024));
        }
        return Math.max(1, Math.round(bytes / 1024d)) + " Ko";
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
            at(bold, 8.5f, MUTED, x, atY, key);
            String v = value == null || value.isBlank() ? "—" : value;
            at(regular, 9.5f, INK, x + 96, atY, v.length() > PAIR_MAX ? v.substring(0, PAIR_MAX - 1) + "…" : v);
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
                    case '–', '—', '…', '•' -> b.append(ch);
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
