package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.billing.BillingProfileDto;
import com.openlab.qualitos.core.billing.Money;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Rend la facture en PDF A4, avec Apache PDFBox — même moteur et même idiome
 * que {@code PdfBoxDashboardRenderAdapter} dans le moteur de qualité.
 *
 * <p><b>La pièce porte l'identité du client, jamais son UUID.</b> C'est la
 * raison d'être du profil de facturation : un identifiant technique sur une
 * facture ne désigne personne pour un comptable, et ne vaut rien devant un
 * contrôle.
 *
 * <p><b>Les montants s'écrivent avec leurs deux décimales et leur devise.</b>
 * Les montants vivent en centimes entiers ; imprimer « 9900 » là où il faut
 * « 99,00 EUR » est l'erreur que toute la chaîne de tests laisse passer, parce
 * qu'aucune assertion sur des entiers ne la voit. D'où
 * {@link InvoiceAmounts#format}, partagé avec le corps du courriel — un seul
 * endroit où la règle est écrite.
 *
 * <p><b>Le texte est réduit au jeu WinAnsi.</b> Les polices standard de PDFBox
 * ne savent pas dessiner tous les caractères Unicode, et une raison sociale
 * saisie avec un tiret cadratin ou une apostrophe typographique ferait ÉCHOUER
 * l'écriture — donc l'émission d'une facture, pour une question de ponctuation.
 * On translittère ce qu'on peut, on remplace le reste ; la facture sort.
 */
@Component
public class PdfBoxInvoiceRenderer implements InvoiceRenderPort {

    private static final DateTimeFormatter ISSUED_AT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);

    private static final float MARGIN = 48f;
    private static final float WIDTH = PDRectangle.A4.getWidth();
    private static final float HEIGHT = PDRectangle.A4.getHeight();

    // Palette QualitOS (indigo sur clair), identique à l'export de tableau de bord.
    private static final float[] INDIGO = {0.224f, 0.286f, 0.671f};
    private static final float[] INK = {0.102f, 0.102f, 0.180f};
    private static final float[] MUTED = {0.45f, 0.45f, 0.50f};

    // Colonnes du tableau des lignes, en points depuis la marge gauche.
    private static final float COL_MODULE = MARGIN;
    private static final float COL_TIER = MARGIN + 190f;
    private static final float COL_PERIOD = MARGIN + 275f;
    private static final float COL_QTY = MARGIN + 360f;
    private static final float COL_TOTAL = MARGIN + 400f;

    @Override
    public byte[] render(Invoice invoice, BillingProfileDto.View profile) {
        try (PDDocument document = new PDDocument()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = HEIGHT - MARGIN;
                y = drawHeader(content, bold, regular, invoice, y);
                y = drawCustomer(content, bold, regular, profile, y);
                y = drawLines(content, bold, regular, invoice, y);
                drawTotal(content, bold, invoice, y);
                drawFooter(content, regular, invoice);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            // Une facture qu'on ne sait pas rendre n'est pas une facture
            // fausse : c'est une panne de mise en page. On la propage plutôt
            // que rendre un PDF vide, qui partirait au client.
            throw new UncheckedIOException("rendu PDF de la facture impossible", e);
        }
    }

    private float drawHeader(PDPageContentStream content, PDType1Font bold, PDType1Font regular,
                             Invoice invoice, float y) throws IOException {
        text(content, bold, 24, INDIGO, MARGIN, y, "FACTURE");
        text(content, bold, 14, INK, WIDTH - MARGIN - 150, y, sanitize(invoice.getNumber()));
        y -= 10;
        line(content, INDIGO, MARGIN, y, WIDTH - MARGIN, y, 2f);
        y -= 20;
        text(content, regular, 10, MUTED, MARGIN, y,
                "Periode facturee : " + invoice.period());
        text(content, regular, 10, MUTED, WIDTH - MARGIN - 150, y,
                "Emise le " + ISSUED_AT.format(invoice.getIssuedAt()));
        return y - 34;
    }

    private float drawCustomer(PDPageContentStream content, PDType1Font bold, PDType1Font regular,
                               BillingProfileDto.View profile, float y) throws IOException {
        text(content, bold, 11, MUTED, MARGIN, y, "FACTURE A");
        y -= 16;
        // La raison sociale, pas l'UUID : c'est toute la raison d'etre du profil.
        text(content, bold, 13, INK, MARGIN, y, sanitize(profile.legalName()));
        y -= 15;
        y = textLine(content, regular, 10, MARGIN, y, profile.addressLine1());
        y = textLine(content, regular, 10, MARGIN, y, profile.addressLine2());
        y = textLine(content, regular, 10, MARGIN, y,
                profile.postalCode() + " " + profile.city() + " (" + profile.countryCode() + ")");
        // Le numero de TVA n'existe pas pour tous les clients : on l'imprime
        // quand il existe, on ne laisse pas une ligne « TVA : » vide, qui ferait
        // croire a une omission.
        y = textLine(content, regular, 10, MARGIN, y,
                isBlank(profile.vatNumber()) ? null : "N TVA : " + profile.vatNumber());
        return y - 22;
    }

    private float drawLines(PDPageContentStream content, PDType1Font bold, PDType1Font regular,
                            Invoice invoice, float y) throws IOException {
        text(content, bold, 10, MUTED, COL_MODULE, y, "MODULE");
        text(content, bold, 10, MUTED, COL_TIER, y, "PALIER");
        text(content, bold, 10, MUTED, COL_PERIOD, y, "PERIODICITE");
        text(content, bold, 10, MUTED, COL_QTY, y, "QTE");
        text(content, bold, 10, MUTED, COL_TOTAL, y, "MONTANT");
        y -= 6;
        line(content, MUTED, MARGIN, y, WIDTH - MARGIN, y, 0.5f);
        y -= 16;

        for (InvoiceLine invoiceLine : invoice.getLines()) {
            text(content, regular, 10, INK, COL_MODULE, y, sanitize(invoiceLine.getModuleCode()));
            text(content, regular, 10, INK, COL_TIER, y, invoiceLine.getBillingTier().name());
            text(content, regular, 10, INK, COL_PERIOD, y, invoiceLine.getPeriod().name());
            text(content, regular, 10, INK, COL_QTY, y, String.valueOf(invoiceLine.getQuantity()));
            text(content, regular, 10, INK, COL_TOTAL, y, InvoiceAmounts.format(
                    Money.of(invoiceLine.getLineTotalCents(), invoice.getCurrency())));
            y -= 15;
        }
        return y - 8;
    }

    private void drawTotal(PDPageContentStream content, PDType1Font bold,
                           Invoice invoice, float y) throws IOException {
        line(content, INDIGO, COL_PERIOD, y, WIDTH - MARGIN, y, 1f);
        y -= 18;
        text(content, bold, 12, INK, COL_PERIOD, y, "TOTAL");
        text(content, bold, 12, INDIGO, COL_TOTAL, y, InvoiceAmounts.format(invoice.total()));
    }

    private void drawFooter(PDPageContentStream content, PDType1Font regular,
                            Invoice invoice) throws IOException {
        text(content, regular, 8, MUTED, MARGIN, MARGIN,
                "QualitOS - facture " + sanitize(invoice.getNumber())
                        + " - numerotation continue par exercice " + invoice.getFiscalYear());
    }

    // ---------- primitives de dessin ----------

    private float textLine(PDPageContentStream content, PDType1Font font, float size,
                           float x, float y, String value) throws IOException {
        if (isBlank(value)) {
            return y;
        }
        text(content, font, size, INK, x, y, sanitize(value));
        return y - 14;
    }

    private void text(PDPageContentStream content, PDType1Font font, float size,
                      float[] color, float x, float y, String value) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.setNonStrokingColor(color[0], color[1], color[2]);
        content.newLineAtOffset(x, y);
        content.showText(value);
        content.endText();
    }

    private void line(PDPageContentStream content, float[] color,
                      float x1, float y1, float x2, float y2, float width) throws IOException {
        content.setStrokingColor(color[0], color[1], color[2]);
        content.setLineWidth(width);
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    /**
     * Réduit le texte au jeu que les polices standard savent dessiner.
     *
     * <p>Les apostrophes et tirets typographiques sont translittérés vers leur
     * équivalent ASCII plutôt que remplacés par un point d'interrogation :
     * « L'Étoile » reste lisible, alors que « L?Étoile » ne l'est plus. Le
     * reste devient un point d'interrogation — visible, donc corrigeable, là où
     * une exception aurait empêché la facture de sortir.
     */
    /**
     * L'apostrophe droite, nommee plutot qu'ecrite en litteral de caractere.
     *
     * <p>Un litteral en sequence unicode serait un piege : javac interprete
     * ces sequences AVANT l'analyse lexicale, si bien que la ligne
     * deviendrait trois apostrophes accolees et ne compilerait pas. La forme
     * echappee compile, mais se relit mal au milieu d'un switch qui ne
     * contient par ailleurs que des caracteres typographiques.
     */
    private static final char APOSTROPHE = 39;

    static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '‘', '’', 'ʼ' -> out.append(APOSTROPHE);
                case '“', '”' -> out.append('"');
                case '–', '—', '−' -> out.append('-');
                case '…' -> out.append("...");
                case ' ', ' ', ' ' -> out.append(' ');
                case '€' -> out.append("EUR");
                default -> out.append(isDrawable(c) ? c : '?');
            }
        }
        return out.toString();
    }

    /**
     * Les caracteres que WinAnsiEncoding sait dessiner.
     *
     * <p>La plage 0x80-0x9F est EXCLUE a dessein : elle n'est pas de
     * l'ASCII etendu, et WinAnsiEncoding n'y definit pas tous les codes.
     * Les y laisser passer sous pretexte qu'ils tiennent sur un octet
     * ferait lever une exception a l'ecriture — c'est-a-dire echouer
     * l'emission d'une facture, pour un caractere invisible colle depuis
     * un traitement de texte.
     */
    private static boolean isDrawable(char c) {
        return (c >= 0x20 && c <= 0x7E) || (c >= 0xA0 && c <= 0xFF);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
