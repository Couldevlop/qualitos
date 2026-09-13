package com.openlab.qualitos.quality.nonconformity.eightd.infrastructure;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDPdfRenderPort;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDSnapshot;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Met le rapport 8D en page : une couverture, les huit disciplines à la suite sur
 * autant de pages que nécessaire, un pied d'intégrité avec QR code de vérification.
 *
 * <p><b>Rendu déterministe</b>, contrainte première de cette classe : l'empreinte
 * scellée à l'émission porte sur ces octets, et doit rester recalculable à
 * l'identique. D'où trois précautions qu'il ne faut pas défaire :
 * <ul>
 *   <li>aucune lecture d'horloge — les dates viennent de l'instantané, déjà en texte ;</li>
 *   <li>les métadonnées du document (dates de création/modification, producteur)
 *       sont posées à des valeurs FIXES : PDFBox y mettrait sinon l'heure courante ;</li>
 *   <li>l'identifiant de fichier ({@code /ID} du trailer) est DÉRIVÉ du contenu.
 *       PDFBox en tire sinon un nombre aléatoire à chaque enregistrement, et c'était
 *       la seule différence entre deux rendus du même rapport — trente octets qui
 *       suffisaient à faire échouer la comparaison d'empreinte ;</li>
 *   <li>polices standard seulement, donc aucune dépendance à ce qui est installé
 *       sur la machine.</li>
 * </ul>
 *
 * <p>Le texte est ramené au jeu WinAnsi : les polices standard ne savent pas
 * encoder un caractère arbitraire, et un glyphe exotique ferait échouer le rendu
 * au lieu de dégrader l'accent.
 */
@Component
public class PdfBoxEightDRenderAdapter implements EightDPdfRenderPort {

    // Palette QualitOS (indigo sur fond clair), alignée sur l'export de dashboard.
    private static final float[] INDIGO = {0.224f, 0.286f, 0.671f};
    private static final float[] INK = {0.102f, 0.102f, 0.180f};
    private static final float[] MUTED = {0.45f, 0.45f, 0.50f};
    private static final float[] ALERT = {0.702f, 0.149f, 0.149f};
    private static final float[] PANEL = {0.96f, 0.97f, 0.99f};

    private static final float MARGIN = 48f;
    private static final float WIDTH = PDRectangle.A4.getWidth();
    private static final float HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_W = WIDTH - 2 * MARGIN;

    /** Hauteur réservée au pied d'intégrité de la DERNIÈRE page. */
    private static final float FOOTER_H = 150f;

    /** Largeur en caractères d'une ligne de corps, à 9,5 pt sur la laisse utile. */
    private static final int WRAP = 104;

    /**
     * Date figée des métadonnées PDF. Une constante, et non « maintenant » : voir le
     * contrat de déterminisme du port.
     */
    private static final Calendar EPOCH = epoch();

    private static Calendar epoch() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
        return calendar;
    }

    @Override
    public byte[] render(EightDSnapshot snapshot, String verifyUrl) {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font mono = new PDType1Font(Standard14Fonts.FontName.COURIER);

            figerMetadonnees(doc, snapshot);

            Page page = nouvellePage(doc);
            page.y = entete(page.flux, bold, regular, snapshot, page.y);
            page.y = encadreNc(page.flux, bold, regular, snapshot, page.y);

            for (EightDSnapshot.Section section : snapshot.sections()) {
                page = discipline(doc, page, bold, regular, section);
            }

            piedIntegrite(doc, page, bold, regular, mono, snapshot, verifyUrl);
            page.flux.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("rendu du rapport 8D impossible", e);
        }
    }

    /** Une page ouverte et le curseur vertical qui la parcourt. */
    private static final class Page {
        private final PDPageContentStream flux;
        private float y;

        private Page(PDPageContentStream flux, float y) {
            this.flux = flux;
            this.y = y;
        }
    }

    private Page nouvellePage(PDDocument doc) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        return new Page(new PDPageContentStream(doc, page), HEIGHT - MARGIN);
    }

    private void figerMetadonnees(PDDocument doc, EightDSnapshot snapshot) {
        doc.getDocumentInformation().setTitle("Rapport 8D " + sanitize(snapshot.ncReference()));
        doc.getDocumentInformation().setSubject(sanitize(snapshot.ncTitle()));
        doc.getDocumentInformation().setProducer("QualitOS");
        doc.getDocumentInformation().setCreator("QualitOS");
        doc.getDocumentInformation().setCreationDate(EPOCH);
        doc.getDocumentInformation().setModificationDate(EPOCH);
        // /ID dérivé du contenu : stable d'un rendu à l'autre, distinct d'un rapport
        // à l'autre. Un identifiant constant aurait aussi été déterministe, mais deux
        // documents différents porteraient alors le même.
        COSString identifiant = new COSString(empreinteCourte(
                snapshot.ncReference() + "|" + snapshot.ncTitle()
                + "|" + snapshot.issuedAtText()));
        doc.getDocument().setDocumentID(new COSArray(java.util.List.of(identifiant, identifiant)));
    }

    /** Seize octets tirés d'un SHA-256 du contenu : assez pour un identifiant de fichier. */
    private static byte[] empreinteCourte(String graine) {
        try {
            byte[] complet = MessageDigest.getInstance("SHA-256")
                    .digest(graine.getBytes(StandardCharsets.UTF_8));
            byte[] court = new byte[16];
            System.arraycopy(complet, 0, court, 0, 16);
            return court;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private float entete(PDPageContentStream c, PDType1Font bold, PDType1Font regular,
                         EightDSnapshot snapshot, float y) throws IOException {
        texte(c, bold, 22, INDIGO, MARGIN, y, "Rapport 8D");
        texte(c, regular, 11, MUTED, MARGIN + 120, y,
                sanitize(snapshot.ncReference()));
        y -= 8;
        ligne(c, INDIGO, MARGIN, y, WIDTH - MARGIN, y, 2f);
        y -= 18;
        texte(c, regular, 10, MUTED, MARGIN, y,
                "Resolution de probleme en huit disciplines - QualitOS"
                + " (signe ML-DSA + ancre blockchain)");
        y -= 16;
        if (snapshot.partial()) {
            // Dit en tête, pas en note de bas de page : un lecteur doit savoir avant
            // de lire que le document est incomplet, et lesquelles de ses parties
            // n'ont pas de contenu.
            texte(c, bold, 10, ALERT, MARGIN, y,
                    "RAPPORT PARTIEL - disciplines sans contenu : "
                    + String.join(", ", snapshot.missingCodes()));
            y -= 16;
        }
        return y - 10;
    }

    private float encadreNc(PDPageContentStream c, PDType1Font bold, PDType1Font regular,
                            EightDSnapshot snapshot, float y) throws IOException {
        float hauteur = 62f;
        rect(c, PANEL, MARGIN, y - hauteur, CONTENT_W, hauteur);
        float x = MARGIN + 14;
        float ligne = y - 18;
        cle(c, bold, regular, x, ligne, "Non-conformite", snapshot.ncReference());
        cle(c, bold, regular, x, ligne - 18, "Intitule", snapshot.ncTitle());
        cle(c, bold, regular, x, ligne - 36, "Emis le",
                snapshot.issuedAtText() == null ? "(brouillon)" : snapshot.issuedAtText());
        return y - hauteur - 22;
    }

    private Page discipline(PDDocument doc, Page page, PDType1Font bold, PDType1Font regular,
                            EightDSnapshot.Section section) throws IOException {
        page = place(doc, page, 46f);
        texte(page.flux, bold, 13, INDIGO, MARGIN, page.y,
                section.code() + " - " + sanitize(section.title()));
        page.y -= 14;
        // Le libellé de source est rouge quand il n'y a RIEN : la discipline vide
        // s'explique au lieu de se taire.
        texte(page.flux, regular, 8.5f, section.sourced() ? MUTED : ALERT, MARGIN, page.y,
                sanitize(section.sourceLabel()));
        page.y -= 14;

        if (section.lines().isEmpty()) {
            page = place(doc, page, 16f);
            texte(page.flux, regular, 9.5f, ALERT, MARGIN + 10, page.y, "Non renseigne.");
            page.y -= 14;
            return page;
        }
        for (String brute : section.lines()) {
            for (String morceau : couper(brute)) {
                page = place(doc, page, 13f);
                texte(page.flux, regular, 9.5f, INK, MARGIN + 10, page.y, morceau);
                page.y -= 12.5f;
            }
        }
        page.y -= 8;
        return page;
    }

    /**
     * Garantit {@code besoin} points de place sous le curseur, en changeant de page
     * au besoin. La dernière page réserve en plus la hauteur du pied d'intégrité.
     */
    private Page place(PDDocument doc, Page page, float besoin) throws IOException {
        if (page.y - besoin >= MARGIN + FOOTER_H) {
            return page;
        }
        page.flux.close();
        return nouvellePage(doc);
    }

    private void piedIntegrite(PDDocument doc, Page page, PDType1Font bold, PDType1Font regular,
                               PDType1Font mono, EightDSnapshot snapshot, String verifyUrl)
            throws IOException {
        PDPageContentStream c = page.flux;
        float haut = MARGIN + FOOTER_H;
        ligne(c, MUTED, MARGIN, haut, WIDTH - MARGIN, haut, 0.5f);
        float y = haut - 18;
        texte(c, bold, 12, INDIGO, MARGIN, y, "Integrite & verification");
        y -= 15;
        texte(c, regular, 9, INK, MARGIN, y,
                "Ce rapport est signe (Ed25519 + ML-DSA-65) et son empreinte est ancree.");
        y -= 13;
        texte(c, regular, 9, INK, MARGIN, y,
                "Son contenu est fige : il dit ce qu'il disait le jour de l'emission.");
        y -= 13;
        if (snapshot.issuedByName() != null) {
            texte(c, regular, 9, MUTED, MARGIN, y, "Emis par : " + sanitize(snapshot.issuedByName()));
        }
        texte(c, regular, 8, MUTED, MARGIN, MARGIN + 30,
                "Scannez le QR code pour verifier l'authenticite de ce document sur QualitOS.");
        if (snapshot.tenantLabel() != null) {
            texte(c, mono, 7.5f, MUTED, MARGIN, MARGIN + 16, "Tenant : " + snapshot.tenantLabel());
        }

        float taille = 110f;
        PDImageXObject qr = qrCode(doc, verifyUrl, taille);
        c.drawImage(qr, WIDTH - MARGIN - taille, MARGIN + 12, taille, taille);
    }

    // ---- dessin de bas niveau ----

    private void cle(PDPageContentStream c, PDType1Font bold, PDType1Font regular,
                     float x, float y, String cle, String valeur) throws IOException {
        texte(c, bold, 9, MUTED, x, y, cle);
        texte(c, regular, 10, INK, x + 110, y, sanitize(valeur));
    }

    private void texte(PDPageContentStream c, PDType1Font font, float taille,
                       float[] couleur, float x, float y, String s) throws IOException {
        c.beginText();
        c.setFont(font, taille);
        c.setNonStrokingColor(couleur[0], couleur[1], couleur[2]);
        c.newLineAtOffset(x, y);
        c.showText(sanitize(s));
        c.endText();
    }

    private void ligne(PDPageContentStream c, float[] couleur, float x1, float y1,
                       float x2, float y2, float epaisseur) throws IOException {
        c.setStrokingColor(couleur[0], couleur[1], couleur[2]);
        c.setLineWidth(epaisseur);
        c.moveTo(x1, y1);
        c.lineTo(x2, y2);
        c.stroke();
    }

    private void rect(PDPageContentStream c, float[] couleur, float x, float y,
                      float w, float h) throws IOException {
        c.setNonStrokingColor(couleur[0], couleur[1], couleur[2]);
        c.addRect(x, y, w, h);
        c.fill();
    }

    /**
     * Coupe une ligne trop longue, en préservant l'indentation d'origine : les
     * sous-points d'un PFMEA ou d'un 5 pourquoi perdraient sinon leur hiérarchie à
     * la première ligne repliée.
     */
    private List<String> couper(String texte) {
        String valeur = texte == null ? "" : texte;
        List<String> morceaux = new ArrayList<>();
        String indentation = valeur.substring(0, valeur.length() - valeur.stripLeading().length());
        String reste = valeur.strip();
        if (reste.isEmpty()) {
            morceaux.add(indentation);
            return morceaux;
        }
        int largeur = Math.max(20, WRAP - indentation.length());
        boolean premiere = true;
        while (!reste.isEmpty()) {
            if (reste.length() <= largeur) {
                morceaux.add(indentation + (premiere ? "" : "  ") + reste);
                break;
            }
            int coupe = reste.lastIndexOf(' ', largeur);
            if (coupe <= 0) {
                coupe = largeur;
            }
            morceaux.add(indentation + (premiere ? "" : "  ") + reste.substring(0, coupe).stripTrailing());
            reste = reste.substring(coupe).stripLeading();
            premiere = false;
        }
        return morceaux;
    }

    private PDImageXObject qrCode(PDDocument doc, String contenu, float px) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            int taille = Math.round(px) * 2;
            BitMatrix matrice = new QRCodeWriter().encode(
                    contenu, BarcodeFormat.QR_CODE, taille, taille, hints);
            BufferedImage image = new BufferedImage(taille, taille, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < taille; x++) {
                for (int y = 0; y < taille; y++) {
                    image.setRGB(x, y, matrice.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            return LosslessFactory.createFromImage(doc, image);
        } catch (Exception e) {
            throw new IllegalStateException("generation du QR code impossible", e);
        }
    }

    /** Ramène au jeu WinAnsi des polices standard, en dégradant les accents. */
    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 32 && ch < 127) {
                b.append(ch);
            } else {
                switch (ch) {
                    case 'é', 'è', 'ê', 'ë' -> b.append('e');
                    case 'à', 'â', 'ä' -> b.append('a');
                    case 'î', 'ï' -> b.append('i');
                    case 'ô', 'ö' -> b.append('o');
                    case 'û', 'ü', 'ù' -> b.append('u');
                    case 'ç' -> b.append('c');
                    case 'É', 'È', 'Ê' -> b.append('E');
                    case 'À', 'Â' -> b.append('A');
                    case 'Ç' -> b.append('C');
                    case '’', '‘' -> b.append('\'');
                    case '–', '—' -> b.append('-');
                    case '…' -> b.append("...");
                    case ' ' -> b.append(' ');
                    default -> b.append('?');
                }
            }
        }
        return b.toString();
    }
}
