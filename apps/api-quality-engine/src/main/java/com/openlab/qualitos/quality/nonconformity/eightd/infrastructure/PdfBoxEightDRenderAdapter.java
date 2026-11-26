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
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
 * <p>Le texte est ramené à ce que ces polices savent encoder — voir
 * {@link #sanitize(String)}. Elles couvrent tout le français ; seul ce qui sort
 * de leur jeu est remplacé, faute de quoi un glyphe exotique ferait échouer le
 * rendu au lieu d'abîmer un mot.
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

    /** Ce que les polices du document savent ecrire ; voir {@link #sanitize(String)}. */
    private static final BitSet ENCODABLE = encodableParLesPolices();

    /** Hauteur réservée au pied d'intégrité de la DERNIÈRE page. */
    private static final float FOOTER_H = 150f;

    /**
     * Marge basse d'une page de CONTENU.
     *
     * <p>À ne pas confondre avec {@link #FOOTER_H}. Le pied d'intégrité ne
     * s'écrit que sur la dernière page ; réserver sa hauteur sur toutes
     * amputait chaque page d'un tiers, et poussait à la page suivante ce qui
     * tenait largement — on a vu ainsi la cause racine d'une descente atterrir
     * seule en haut d'une page, détachée du raisonnement dont elle conclut.
     */
    private static final float BOTTOM_GUTTER = 30f;

    /** Largeur de la tête du poisson : l'énoncé y tient, hors des branches. */
    private static final float TETE_W = 108f;

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

            boolean fr = parleFrancais(snapshot);
            figerMetadonnees(doc, snapshot, fr);

            Page page = nouvellePage(doc);
            page.y = entete(page.flux, bold, regular, snapshot, page.y, fr);
            page.y = encadreNc(page.flux, bold, regular, snapshot, page.y, fr);

            for (EightDSnapshot.Section section : snapshot.sections()) {
                page = discipline(doc, page, bold, regular, section, fr);
            }

            page = piedIntegrite(doc, page, bold, regular, mono, snapshot, verifyUrl, fr);
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

    private void figerMetadonnees(PDDocument doc, EightDSnapshot snapshot, boolean fr) {
        doc.getDocumentInformation().setTitle(
                mot(fr, "Rapport 8D ", "8D Report ") + sanitize(snapshot.ncReference()));
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
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private float entete(PDPageContentStream c, PDType1Font bold, PDType1Font regular,
                         EightDSnapshot snapshot, float y, boolean fr) throws IOException {
        texte(c, bold, 22, INDIGO, MARGIN, y, mot(fr, "Rapport 8D", "8D Report"));
        texte(c, regular, 11, MUTED, MARGIN + 120, y,
                sanitize(snapshot.ncReference()));
        y -= 8;
        ligne(c, INDIGO, MARGIN, y, WIDTH - MARGIN, y, 2f);
        y -= 18;
        texte(c, regular, 10, MUTED, MARGIN, y, mot(fr,
                "Résolution de problème en huit disciplines - QualitOS"
                + " (signé ML-DSA + ancré blockchain)",
                "Eight-discipline problem solving - QualitOS"
                + " (ML-DSA signed + blockchain anchored)"));
        y -= 16;
        // UN BROUILLON SE RECONNAIT A SA PREMIERE LIGNE. Le document se
        // telecharge desormais avant l'emission -- pour se relire, se faire
        // relire, se corriger. Remis tel quel a un client, il passerait pour le
        // rapport opposable : il le dit donc lui-meme, en tete, avant son
        // contenu.
        if (snapshot.issuedAtText() == null) {
            texte(c, bold, 10, ALERT, MARGIN, y, mot(fr,
                    "BROUILLON - document de travail, non emis : ni signe, ni ancre",
                    "DRAFT - work in progress, not issued: neither signed nor anchored"));
            y -= 16;
        }
        if (snapshot.partial()) {
            // Dit en tête, pas en note de bas de page : un lecteur doit savoir avant
            // de lire que le document est incomplet, et lesquelles de ses parties
            // n'ont pas de contenu.
            texte(c, bold, 10, ALERT, MARGIN, y,
                    mot(fr, "RAPPORT PARTIEL - disciplines sans contenu : ",
                        "PARTIAL REPORT - disciplines with no content: ")
                    + String.join(", ", snapshot.missingCodes()));
            y -= 16;
        }
        return y - 10;
    }

    private float encadreNc(PDPageContentStream c, PDType1Font bold, PDType1Font regular,
                            EightDSnapshot snapshot, float y, boolean fr) throws IOException {
        float hauteur = 62f;
        rect(c, PANEL, MARGIN, y - hauteur, CONTENT_W, hauteur);
        float x = MARGIN + 14;
        float ligne = y - 18;
        cle(c, bold, regular, x, ligne, mot(fr, "Non-conformité", "Non-conformity"),
                snapshot.ncReference());
        cle(c, bold, regular, x, ligne - 18, mot(fr, "Intitulé", "Title"),
                snapshot.ncTitle());
        cle(c, bold, regular, x, ligne - 36, mot(fr, "Émis le", "Issued on"),
                snapshot.issuedAtText() == null
                        ? mot(fr, "(brouillon)", "(draft)") : snapshot.issuedAtText());
        return y - hauteur - 22;
    }

    private Page discipline(PDDocument doc, Page page, PDType1Font bold, PDType1Font regular,
                            EightDSnapshot.Section section, boolean fr) throws IOException {
        page = place(doc, page, 46f);
        texte(page.flux, bold, 13, INDIGO, MARGIN, page.y,
                section.code() + " - " + sanitize(section.title()));
        page.y -= 14;
        // LE LIBELLÉ DE PROVENANCE N'APPARAÎT QUE S'IL Y EN A UN, et il n'y en
        // a plus que pour la discipline VIDE, qui s'explique au lieu de se
        // taire -- d'où le rouge. Servie, elle le laisse vide : la ligne ne se
        // réserve alors pas, sans quoi un blanc de 14 points séparerait chaque
        // titre de son contenu. Les rapports scellés avant cette règle portent
        // tous un libellé et se rendent donc à l'identique.
        if (!section.sourceLabel().isBlank()) {
            texte(page.flux, regular, 8.5f, section.sourced() ? MUTED : ALERT, MARGIN, page.y,
                    sanitize(section.sourceLabel()));
            page.y -= 14;
        }

        if (rienAMontrer(section)) {
            page = place(doc, page, 16f);
            texte(page.flux, regular, 9.5f, ALERT, MARGIN + 10, page.y,
                    mot(fr, "Non renseignée.", "Not filled in."));
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
        for (EightDSnapshot.Figure figure : section.figures()) {
            page = figure(doc, page, bold, regular, figure, fr);
        }
        for (EightDSnapshot.Table actions : section.tables()) {
            page = tableau(doc, page, bold, regular, actions);
        }
        page.y -= 8;
        return page;
    }

    /**
     * Une section vide de texte MAIS porteuse d'une figure n'est pas vide.
     *
     * <p>Sans cette question, le D4 d'un ecart dont la cause s'est cherchee en
     * Ishikawa sans etre recopiee sur la fiche aurait affiche « Not filled in. »
     * au-dessus de son propre trace.
     */
    private static boolean rienAMontrer(EightDSnapshot.Section section) {
        return section.lines().isEmpty() && section.figures().isEmpty()
                && section.tables().isEmpty();
    }


    // ---------- les tableaux ----------

    /**
     * Les parts de largeur des colonnes d'actions, dans l'ordre de l'ecran.
     *
     * <p>L'intitule prend le tiers : c'est le seul champ de phrase, les cinq
     * autres sont des mots ou des dates. Donner a tous la meme largeur
     * repliait l'intitule sur quatre lignes a cote de colonnes aux trois
     * quarts vides.
     *
     * <p>Les parts sont calees sur les valeurs les plus LONGUES qui existent,
     * pas sur les plus courantes : « CONTAINMENT » et « IN_PROGRESS » sont des
     * valeurs d'enumeration, elles reviennent telles quelles et, trop serrees,
     * venaient toucher la colonne suivante -- deux valeurs qui se touchent se
     * lisent comme une seule.
     */
    private static final float[] PARTS_COLONNES = {0.15f, 0.32f, 0.15f, 0.12f, 0.16f, 0.10f};

    /**
     * Les parts d'un tableau d'actions SANS la colonne des natures, qui est
     * desormais la forme de toutes les disciplines d'actions : intitule,
     * responsable, echeance, etat, preuves.
     *
     * <p>L'intitule prend les deux cinquiemes : c'est le seul champ de phrase,
     * et la part de la colonne retiree lui revient d'abord -- elle se repliait
     * sur quatre lignes a cote de colonnes aux trois quarts vides.
     */
    private static final float[] PARTS_ACTIONS_SANS_NATURE =
            {0.40f, 0.20f, 0.15f, 0.15f, 0.10f};

    /**
     * Les memes, sans la colonne des preuves : les parts du D5.
     *
     * <p>Cette discipline ne porte pas de piece par action -- ce qu'elle a a
     * demontrer se prouve au D6, sur le dossier.
     */
    private static final float[] PARTS_ACTIONS_D5 = {0.45f, 0.22f, 0.16f, 0.17f};

    /**
     * Les parts du tableau de l'equipe, au D1 : nom, adresse, role.
     *
     * <p>L'adresse est la plus longue des trois -- un prenom.nom suivi d'un
     * domaine --, et le role tient en un mot.
     */
    private static final float[] PARTS_EQUIPE = {0.35f, 0.42f, 0.23f};

    /** La hauteur d'une ligne de tableau, par ligne de texte repliee. */
    private static final float LIGNE_CELLULE = 11f;

    /**
     * Trace un tableau : un en-tete, puis une ligne par action.
     *
     * <p>L'ecran presente les actions decidees en colonnes, et le document emis
     * doit se presenter comme l'ecran. Les memes donnees en phrases -- « Action
     * [CORRECTIVE] Remplacer le joint - DONE - due 12/09 » -- ne se comparent
     * pas d'une ligne a l'autre, et c'est pourtant ce qu'on vient faire dans
     * ces disciplines : voir d'un coup d'oeil ce qui reste ouvert, ce qui est
     * en retard, ce qui n'est pas prouve.
     *
     * <p>UNE LIGNE NE SE COUPE PAS en deux pages, et l'EN-TETE SE REPETE sur la
     * page suivante : une colonne de valeurs sans son intitule ne se lit plus,
     * et pire, elle se lit de travers.
     */
    private Page tableau(PDDocument doc, Page page, PDType1Font bold, PDType1Font regular,
                         EightDSnapshot.Table tableau) throws IOException {
        float[] largeurs = largeursDe(tableau.columns().size());
        // Le titre et l'en-tete tiennent ensemble, avec au moins une ligne :
        // un dossier annonce en bas de page dont le tableau commence a la
        // suivante se lirait comme un dossier vide.
        page = place(doc, page, 18f + LIGNE_CELLULE + 2 * LIGNE_CELLULE);
        page.y -= 4;
        // UN TABLEAU SANS TITRE N'EN PORTE PAS : celui du D1 est l'équipe
        // elle-même, et « Actions - » au-dessus d'une liste de personnes
        // annoncerait ce qu'il ne montre pas.
        if (!texteOuVide(tableau.caption()).isBlank()) {
            texte(page.flux, bold, 9.5f, INDIGO, MARGIN + 10, page.y,
                    "Actions - " + texteOuVide(tableau.caption()));
            page.y -= 13;
        }
        page = enTete(page, bold, tableau.columns(), largeurs);

        boolean alterne = false;
        for (EightDSnapshot.Row rang : tableau.rows()) {
            List<List<String>> cellules = new ArrayList<>();
            int hautes = 1;
            for (int i = 0; i < rang.cells().size(); i++) {
                List<String> repliee = replier(
                        rang.cells().get(i), regular, 8f, largeurs[i] - 8f);
                cellules.add(repliee);
                hautes = Math.max(hautes, repliee.size());
            }
            float hauteur = hautes * LIGNE_CELLULE + 4f;
            Page avant = page;
            page = place(doc, page, hauteur);
            if (page != avant) {
                // Nouvelle page : l'en-tete se repete, sinon les colonnes
                // perdent leur intitule et se lisent de travers.
                page.y -= 4;
                page = enTete(page, bold, tableau.columns(), largeurs);
                alterne = false;
            }
            if (alterne) {
                rect(page.flux, PANEL, MARGIN + 10, page.y - hauteur + 9.5f,
                        CONTENT_W - 20, hauteur);
            }
            alterne = !alterne;
            float x = MARGIN + 14;
            for (int i = 0; i < cellules.size(); i++) {
                float yc = page.y;
                for (String morceau : cellules.get(i)) {
                    texte(page.flux, regular, 8f, INK, x, yc, morceau);
                    yc -= LIGNE_CELLULE;
                }
                x += largeurs[i];
            }
            page.y -= hauteur;
        }
        ligne(page.flux, MUTED, MARGIN + 10, page.y + 6f,
                WIDTH - MARGIN - 10, page.y + 6f, 0.5f);
        page.y -= 6;
        return page;
    }

    /** L'en-tete des colonnes, souligne. */
    private Page enTete(Page page, PDType1Font bold, List<String> colonnes, float[] largeurs)
            throws IOException {
        float x = MARGIN + 14;
        for (int i = 0; i < colonnes.size(); i++) {
            texte(page.flux, bold, 7.5f, MUTED, x, page.y, colonnes.get(i).toUpperCase(Locale.ROOT));
            x += largeurs[i];
        }
        page.y -= 4;
        ligne(page.flux, MUTED, MARGIN + 10, page.y, WIDTH - MARGIN - 10, page.y, 0.5f);
        page.y -= 11;
        return page;
    }

    /**
     * Les largeurs en points, pour {@code n} colonnes.
     *
     * <p>Les parts connues servent telles quelles ; au-dela, les colonnes se
     * partagent la largeur a egalite -- un tableau d'une autre forme se rendra
     * sobrement plutot que de deborder de la page.
     */
    /**
     * Les parts connues pour {@code n} colonnes, ou {@code null}.
     *
     * <p>Six : un tableau d'actions scelle AVANT que la nature quitte les
     * colonnes. Il se rend exactement comme il a ete emis -- son empreinte
     * tient a cela.
     */
    private static float[] partsDe(int n) {
        return switch (n) {
            case 6 -> PARTS_COLONNES;
            case 5 -> PARTS_ACTIONS_SANS_NATURE;
            case 4 -> PARTS_ACTIONS_D5;
            case 3 -> PARTS_EQUIPE;
            default -> null;
        };
    }

    private static float[] largeursDe(int n) {
        float utile = CONTENT_W - 24f;
        float[] largeurs = new float[n];
        float[] parts = partsDe(n);
        if (parts != null) {
            for (int i = 0; i < n; i++) {
                largeurs[i] = utile * parts[i];
            }
            return largeurs;
        }
        for (int i = 0; i < n; i++) {
            largeurs[i] = utile / n;
        }
        return largeurs;
    }

    // ---------- les figures ----------

    /**
     * Trace une figure, ou la remplace par une phrase si elle n'a aucun noeud.
     *
     * <p>Un diagramme ouvert puis laisse vide existe : il vaut mieux dire qu'il
     * est vide que dessiner une epine nue, qu'on prendrait pour un defaut de
     * rendu.
     */
    private Page figure(PDDocument doc, Page page, PDType1Font bold, PDType1Font regular,
                        EightDSnapshot.Figure figure, boolean fr) throws IOException {
        String titre = figure.kind() == EightDSnapshot.FigureKind.FISHBONE
                ? "Ishikawa - " + texteOuVide(figure.caption())
                : mot(fr, "5 pourquoi - ", "5 whys - ") + texteOuVide(figure.caption());
        // UNE FIGURE NE SE COUPE PAS. Elle vaut par sa forme : une arête dont
        // la moitié des branches passe à la page suivante, ou une descente
        // séparée de sa conclusion, ne montrent plus ce qu'un schéma sert à
        // montrer. On la place donc d'un bloc, titre compris — sauf si elle est
        // plus haute qu'une page entière, auquel cas aucune place ne suffirait
        // et il vaut mieux la commencer ici que gâcher une page pour rien.
        float corps = hauteurDuCorps(figure, regular);
        float utile = HEIGHT - 2 * MARGIN - BOTTOM_GUTTER;
        page = place(doc, page, Math.min(28f + corps, utile));
        page.y -= 6;
        texte(page.flux, bold, 9.5f, INDIGO, MARGIN + 10, page.y, titre);
        page.y -= 12;

        if (figure.nodes().isEmpty()) {
            page = place(doc, page, 14f);
            texte(page.flux, regular, 9f, ALERT, MARGIN + 18, page.y,
                    mot(fr, "Ouverte, mais laissée vide.", "Opened, but left empty."));
            page.y -= 12;
            return page;
        }
        return figure.kind() == EightDSnapshot.FigureKind.FISHBONE
                ? arete(doc, page, bold, regular, figure)
                : echelle(doc, page, bold, regular, figure, fr);
    }

    /**
     * La hauteur qu'une figure occupera, mesuree avant d'etre tracee.
     *
     * <p>C'est elle qui permet de tenir la figure d'un bloc. Elle se calcule
     * avec les memes mesures que le trace -- police comprise -- pour qu'un
     * repli de plus ne fasse pas deborder ce qu'on avait annonce tenir.
     */
    private float hauteurDuCorps(EightDSnapshot.Figure figure, PDType1Font regular)
            throws IOException {
        if (figure.nodes().isEmpty()) {
            return 26f;
        }
        if (figure.kind() == EightDSnapshot.FigureKind.FISHBONE) {
            return disposerArete(figure, regular).hauteur() + 18f;
        }
        float total = 0f;
        for (EightDSnapshot.Node noeud : figure.nodes()) {
            total += 16f + 11f * couperA(noeud.label(), 92).size();
        }
        return total;
    }

    /**
     * La descente des 5 pourquoi : une echelle de cadres, du symptome vers le bas.
     *
     * <p>Chaque reponse est un cadre, relie au suivant par un segment : c'est la
     * descente elle-meme qui porte le sens, et une liste a puces ne la montre
     * pas. Le dernier cadre, quand c'est la cause racine conclue, recoit un
     * filet indigo a gauche -- une descente conclue et une descente interrompue
     * ne se lisent pas pareil.
     *
     * <p>Un cadre ne se coupe JAMAIS entre deux pages : on passe a la page
     * suivante avec lui. Un « pourquoi » a cheval sur une coupure perdrait son
     * rang, qui est tout ce qui le relie a la question precedente.
     */
    private Page echelle(PDDocument doc, Page page, PDType1Font bold, PDType1Font regular,
                         EightDSnapshot.Figure figure, boolean fr) throws IOException {
        float x = MARGIN + 18;
        float largeur = CONTENT_W - 36;
        boolean premier = true;
        List<EightDSnapshot.Node> noeuds = figure.nodes();
        for (int i = 0; i < noeuds.size(); i++) {
            EightDSnapshot.Node noeud = noeuds.get(i);
            List<String> morceaux = couperA(noeud.label(), 92);
            float hauteur = 12f + 11f * morceaux.size();
            float besoin = hauteur + 14f;
            // LA CAUSE RACINE N'EST JAMAIS SEULE EN HAUT D'UNE PAGE. Elle est la
            // conclusion de la descente : detachee de la marche qui la precede,
            // elle n'est plus qu'une affirmation sans son raisonnement -- et
            // c'est justement elle qu'un auditeur vient lire. On exige donc la
            // place des DEUX derniers cadres au moment de poser l'avant-dernier.
            if (i + 1 < noeuds.size() && "ROOT".equals(noeuds.get(i + 1).group())) {
                besoin += 16f + 11f * couperA(noeuds.get(i + 1).label(), 92).size();
            }
            page = place(doc, page, besoin);
            if (!premier) {
                // Le segment de liaison, trace APRES un eventuel saut de page :
                // sinon il pendrait au bas de la page precedente, sous rien.
                ligne(page.flux, MUTED, x + 16, page.y + 10, x + 16, page.y + 2, 0.8f);
            }
            boolean racine = "ROOT".equals(noeud.group());
            rect(page.flux, PANEL, x, page.y - hauteur + 10, largeur, hauteur);
            rect(page.flux, racine ? INDIGO : MUTED, x, page.y - hauteur + 10, 2.5f, hauteur);
            String rang = racine
                    ? mot(fr, "Cause racine", "Root cause")
                    : mot(fr, "Pourquoi ", "Why ") + noeud.depth();
            texte(page.flux, bold, 8f, racine ? INDIGO : MUTED, x + 8, page.y + 1, rang);
            float ligneY = page.y + 1;
            for (String morceau : morceaux) {
                texte(page.flux, regular, 9f, INK, x + 68, ligneY, morceau);
                ligneY -= 11f;
            }
            page.y -= hauteur + 4f;
            premier = false;
        }
        return page;
    }

    /**
     * L'arete de poisson : une epine horizontale, des branches par categorie.
     *
     * <p>Les branches alternent au-dessus et au-dessous de l'epine, comme le
     * diagramme se dessine au tableau ; l'enonce ferme l'epine a droite, dans la
     * tete du poisson. La hauteur est CALCULEE sur le cote le plus charge : une
     * hauteur fixe ferait se chevaucher les causes des qu'une branche en porte
     * plus de trois.
     *
     * <p>Les libelles sont coupes court -- une etiquette de branche doit se lire
     * d'un coup d'oeil, et l'etiquette complete reste sur la fiche de l'ecart.
     */
    private Page arete(PDDocument doc, Page page, PDType1Font bold, PDType1Font regular,
                       EightDSnapshot.Figure figure) throws IOException {
        Arete plan = disposerArete(figure, regular);
        Map<String, List<String>> lignesParBranche = plan.lignesParBranche();
        float gauche = plan.gauche();
        float droite = plan.droite();
        float pas = plan.pas();
        float demiHaut = plan.demiHaut();
        float demiBas = plan.demiBas();
        float hauteur = plan.hauteur();
        float teteLargeur = TETE_W;
        page = place(doc, page, hauteur + 12f);

        float epine = page.y - demiHaut;
        ligne(page.flux, INK, gauche, epine, droite, epine, 1.2f);

        // La tete : l'enonce du probleme, la ou l'epine aboutit. Sa hauteur suit
        // le nombre de lignes repliees, pour ne pas tronquer l'enonce.
        List<String> enonce = replier(figure.caption(), regular, 7.5f, teteLargeur - 14f);
        float teteHauteur = Math.max(28f, 6f + 9f * enonce.size());
        rect(page.flux, PANEL, droite, epine - teteHauteur / 2, teteLargeur, teteHauteur);
        rect(page.flux, INDIGO, droite, epine - teteHauteur / 2, 2.5f, teteHauteur);
        float ligneTete = epine + teteHauteur / 2 - 10f;
        for (String morceau : enonce) {
            texte(page.flux, regular, 7.5f, INK, droite + 7, ligneTete, morceau);
            ligneTete -= 9f;
        }

        int indice = 0;
        for (Map.Entry<String, List<String>> branche : lignesParBranche.entrySet()) {
            boolean haut = indice % 2 == 0;
            float pied = gauche + 24f + pas * indice;
            float pointe = pied - 20f;
            float sommet = haut ? epine + demiHaut - 12f : epine - demiBas + 12f;
            ligne(page.flux, MUTED, pied, epine, pointe, sommet, 0.9f);
            // Le nom de la categorie se pose a la POINTE, au bout de sa branche.
            texte(page.flux, bold, 7.5f, INDIGO, pointe, haut ? sommet + 4 : sommet - 9,
                    branche.getKey());
            // Les causes s'ecrivent a DROITE du pied, donc entierement hors du
            // trait oblique : ecrites a la pointe, elles etaient barrees par leur
            // propre branche. Et elles se lisent toujours de HAUT en BAS, des
            // deux cotes de l'epine -- en bas, les empiler vers le haut faisait
            // lire la derniere ligne en premier.
            float causeY = haut ? sommet - 9f : epine - 12f;
            for (String ligne : branche.getValue()) {
                texte(page.flux, regular, 7f, INK, pied + 3f, causeY, ligne);
                causeY -= 9f;
            }
            indice++;
        }
        page.y -= hauteur + 6f;
        return page;
    }

    /** La geometrie d'une arete, calculee AVANT d'etre tracee, donc mesurable. */
    private record Arete(Map<String, List<String>> lignesParBranche, float gauche, float droite,
                         float pas, float demiHaut, float demiBas) {
        float hauteur() {
            return demiHaut + demiBas;
        }
    }

    /**
     * Dispose les branches d'une arete sans rien tracer.
     *
     * <p>Separer le calcul du trace n'est pas une coquetterie : c'est ce qui
     * permet de savoir si la figure tient sur la page AVANT de l'y commencer.
     */
    private Arete disposerArete(EightDSnapshot.Figure figure, PDType1Font regular)
            throws IOException {
        Map<String, List<String>> parBranche = new LinkedHashMap<>();
        for (EightDSnapshot.Node noeud : figure.nodes()) {
            parBranche.computeIfAbsent(texteOuVide(noeud.group()), k -> new ArrayList<>())
                    .add(noeud.label());
        }

        float gauche = MARGIN + 18;
        // La tete est HORS de la zone des branches, pas superposee a elle : une
        // branche qui finissait sous le cadre de l'enonce le rendait illisible.
        float droite = MARGIN + CONTENT_W - TETE_W - 14;

        int nb = parBranche.size();
        // La zone des branches s'arrete AVANT la tete : la colonne de texte de
        // la derniere branche doit tenir en entier, sinon elle passe sous
        // l'enonce du probleme.
        float pas = (droite - gauche - 24f) / Math.max(1, nb);
        // La largeur d'ECRITURE d'une colonne, en points : c'est elle qui decide
        // du repli. Compter en caracteres coupait « Joint use au-dela de sa
        // duree de vie » en plein mot, sans meme le signaler. La colonne s'arrete
        // avant la pointe de la branche SUIVANTE.
        float colonne = Math.max(38f, pas - 23f);

        // Chaque branche porte ses causes REPLIEES sur sa colonne. La hauteur se
        // calcule donc sur le nombre de LIGNES, pas de causes : sinon deux
        // causes longues se chevauchaient des que l'une tenait sur deux lignes.
        Map<String, List<String>> lignesParBranche = new LinkedHashMap<>();
        int hautMax = 0;
        int basMax = 0;
        int indice = 0;
        for (Map.Entry<String, List<String>> branche : parBranche.entrySet()) {
            List<String> lignes = new ArrayList<>();
            for (String cause : branche.getValue()) {
                lignes.addAll(replier(cause, regular, 7f, colonne));
            }
            lignesParBranche.put(branche.getKey(), lignes);
            if (indice % 2 == 0) {
                hautMax = Math.max(hautMax, lignes.size());
            } else {
                basMax = Math.max(basMax, lignes.size());
            }
            indice++;
        }
        return new Arete(lignesParBranche, gauche, droite, pas,
                30f + 9f * hautMax, 30f + 9f * basMax);
    }

    /**
     * Replie un libelle sur une largeur donnee EN POINTS, mesuree a la police.
     *
     * <p>Compter en caracteres coupait les libelles en plein mot et sans le
     * dire : la colonne d'une branche d'arete est etroite, et c'est la police
     * qui decide ce qui y tient. Un mot plus large que la colonne est laisse
     * entier et depassera -- le tronquer perdrait l'information sans avertir.
     */
    private List<String> replier(String valeur, PDType1Font font, float taille, float largeur)
            throws IOException {
        List<String> lignes = new ArrayList<>();
        String reste = valeur == null ? "" : valeur.strip();
        if (reste.isEmpty()) {
            lignes.add("");
            return lignes;
        }
        StringBuilder courante = new StringBuilder();
        for (String mot : reste.split("\s+")) {
            String essai = courante.isEmpty() ? mot : courante + " " + mot;
            if (!courante.isEmpty() && largeurDe(essai, font, taille) > largeur) {
                lignes.add(courante.toString());
                courante = new StringBuilder(mot);
            } else {
                courante.setLength(0);
                courante.append(essai);
            }
        }
        if (!courante.isEmpty()) {
            lignes.add(courante.toString());
        }
        return lignes;
    }

    /** La largeur du texte REELLEMENT ecrit, c'est-a-dire apres degradation. */
    private float largeurDe(String s, PDType1Font font, float taille) throws IOException {
        return font.getStringWidth(sanitize(s)) / 1000f * taille;
    }

    /** Coupe sur les mots a une largeur donnee ; au moins un morceau, jamais nul. */
    private List<String> couperA(String valeur, int largeur) {
        String reste = valeur == null ? "" : valeur.strip();
        List<String> morceaux = new ArrayList<>();
        if (reste.isEmpty()) {
            morceaux.add("");
            return morceaux;
        }
        while (!reste.isEmpty()) {
            if (reste.length() <= largeur) {
                morceaux.add(reste);
                break;
            }
            int coupe = reste.lastIndexOf(' ', largeur);
            if (coupe <= 0) {
                coupe = largeur;
            }
            morceaux.add(reste.substring(0, coupe).stripTrailing());
            reste = reste.substring(coupe).stripLeading();
        }
        return morceaux;
    }

    private static String texteOuVide(String s) {
        return s == null ? "" : s;
    }

    /**
     * Garantit {@code besoin} points de place sous le curseur, en changeant de page
     * au besoin. La dernière page réserve en plus la hauteur du pied d'intégrité.
     */
    private Page place(PDDocument doc, Page page, float besoin) throws IOException {
        if (page.y - besoin >= MARGIN + BOTTOM_GUTTER) {
            return page;
        }
        page.flux.close();
        return nouvellePage(doc);
    }

    private Page piedIntegrite(PDDocument doc, Page page, PDType1Font bold, PDType1Font regular,
                               PDType1Font mono, EightDSnapshot snapshot, String verifyUrl,
                               boolean fr) throws IOException {
        // Le pied s'écrit au BAS de la dernière page, à une hauteur fixe. Le
        // contenu, lui, descend désormais jusqu'à la marge : s'il atteint cette
        // bande, le pied prend une page à lui. L'écrire par-dessus le texte
        // rendrait illisibles à la fois la preuve et ce qu'elle atteste.
        if (page.y < MARGIN + FOOTER_H + 12f) {
            page.flux.close();
            page = nouvellePage(doc);
        }
        PDPageContentStream c = page.flux;
        float haut = MARGIN + FOOTER_H;
        ligne(c, MUTED, MARGIN, haut, WIDTH - MARGIN, haut, 0.5f);
        float y = haut - 18;
        // SANS URL DE VERIFICATION, IL N'Y A RIEN A VERIFIER : le document n'a
        // pas ete emis. Garder le pied d'integrite tel quel ferait affirmer au
        // brouillon qu'il est signe et ancre, et le QR code menerait a un code
        // qui n'existe pas.
        if (verifyUrl == null) {
            texte(c, bold, 12, ALERT, MARGIN, y,
                    mot(fr, "Brouillon - sans valeur probante", "Draft - no evidentiary value"));
            y -= 15;
            texte(c, regular, 9, INK, MARGIN, y, mot(fr,
                    "Ce document n'a pas ete emis : il n'est ni signe, ni ancre,"
                    + " et son contenu peut encore changer.",
                    "This document has not been issued: it is neither signed nor anchored,"
                    + " and its content may still change."));
            y -= 13;
            texte(c, regular, 9, INK, MARGIN, y, mot(fr,
                    "Le rapport opposable s'obtient en emettant le 8D depuis QualitOS.",
                    "The binding report is obtained by issuing the 8D from QualitOS."));
            if (snapshot.tenantLabel() != null) {
                texte(c, mono, 7.5f, MUTED, MARGIN, MARGIN + 16,
                        "Tenant: " + snapshot.tenantLabel());
            }
            return page;
        }
        texte(c, bold, 12, INDIGO, MARGIN, y,
                mot(fr, "Intégrité et vérification", "Integrity & verification"));
        y -= 15;
        texte(c, regular, 9, INK, MARGIN, y,
                "Ce rapport est signe (Ed25519 + ML-DSA-65) et son empreinte est ancree.");
        y -= 13;
        texte(c, regular, 9, INK, MARGIN, y,
                mot(fr, "Son contenu est figé : il dit ce qu'il disait le jour de son"
                        + " émission.",
                    "Its content is frozen: it says what it said on the day it was issued."));
        y -= 13;
        if (snapshot.issuedByName() != null) {
            texte(c, regular, 9, MUTED, MARGIN, y, mot(fr, "Émis par : ", "Issued by: ")
                    + sanitize(snapshot.issuedByName()));
        }
        texte(c, regular, 8, MUTED, MARGIN, MARGIN + 30,
                "Scannez le QR code pour verifier l'authenticite de ce document sur QualitOS.");
        if (snapshot.tenantLabel() != null) {
            texte(c, mono, 7.5f, MUTED, MARGIN, MARGIN + 16, "Tenant: " + snapshot.tenantLabel());
        }

        float taille = 110f;
        PDImageXObject qr = qrCode(doc, verifyUrl, taille);
        c.drawImage(qr, WIDTH - MARGIN - taille, MARGIN + 12, taille, taille);
        return page;
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

    /**
     * Le document parle-t-il français ?
     *
     * <p>C'est l'INSTANTANÉ qui le dit, jamais la langue du lecteur : l'empreinte
     * scellée porte sur les octets du PDF, et un rapport déjà émis doit se
     * relire à l'octet près. Un instantané scellé avant que le document parle
     * français n'a pas de langue : il garde ses mots d'alors.
     */
    private static boolean parleFrancais(EightDSnapshot snapshot) {
        return "fr".equals(snapshot.langue());
    }

    /** Le mot du document : celui de la langue figee a l'emission. */
    private static String mot(boolean fr, String francais, String anglais) {
        return fr ? francais : anglais;
    }

    /**
     * Ramène le texte à ce que les polices standard savent encoder.
     *
     * <p>Ce qui passe tel quel : tout le français. Les trois polices employées
     * sont en WinAnsiEncoding, qui porte les accents, la cédille, la ligature
     * « œ », l'apostrophe typographique et le tiret cadratin. Les dégrader
     * revenait à abîmer un rapport remis au client pour un risque qui n'existait
     * pas.
     *
     * <p>Ce qui est remplacé par {@code ?} : ce qui sort de ce jeu — « ≤ »,
     * « → », un idéogramme — et les caractères de commande. PDFBox refuse
     * d'écrire un caractère que la police ne sait pas encoder, et cette
     * exception ferait échouer le rendu : perdre un signe vaut mieux que perdre
     * le document.
     *
     * <p>Le jeu accepté est relevé sur les polices elles-mêmes, une fois pour
     * toutes : des 219 caractères de windows-1252 à partir de l'espace, les
     * trois acceptent tout sauf U+007F.
     */
    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            b.append(ENCODABLE.get(ch) ? ch : '?');
        }
        return b.toString();
    }

    /**
     * Les caractères que les polices standard du document savent écrire.
     *
     * <p>Calculé, et non écrit à la main : une table de deux cents entrées
     * tenue à la main se trompe, et elle se tromperait en silence — le rendu
     * échouerait sur un caractère oublié, à l'émission d'un rapport.
     */
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
}
