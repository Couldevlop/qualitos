package com.openlab.qualitos.quality.spc.capability.workbook;

import com.openlab.qualitos.quality.spc.capability.domain.CapabilityCalculation;
import com.openlab.qualitos.quality.spc.capability.domain.ChartFactors;
import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * La grille de mesures d'une étude de capabilité, en classeur Excel : le modèle à
 * remplir, et la lecture du fichier rempli.
 *
 * <p><b>Pourquoi ce chemin plutôt que le collage.</b> La grille accepte déjà un
 * bloc collé depuis un tableur, et cela suffit pour vingt colonnes. Au-delà, les
 * mesures d'un poste de contrôle vivent dans un fichier que personne ne veut
 * ouvrir pour sélectionner la bonne plage : on le verse, et il arrive tel quel.
 * Le modèle à télécharger ferme l'autre moitié du problème — sans lui, chacun
 * invente sa disposition, et l'import devrait devenir devin.
 *
 * <p><b>Côté serveur, et non dans le navigateur.</b> Un .xlsx est une archive :
 * la décompresser dans la page exposerait l'utilisateur aux archives piégées, et
 * demanderait une bibliothèque de plus au bundle. POI est déjà là, avec ses
 * garde-fous de taux de compression (bombes zip).
 *
 * <p>Cette classe ne lit AUCUNE base et n'a aucun état : on lui donne des
 * octets, elle rend une grille. C'est ce qui permet de l'éprouver sur des
 * classeurs fabriqués, sans contexte Spring.
 */
public final class SpcMeasurementsWorkbook {

    /** L'onglet des mesures. Le lecteur le cherche par ce nom avant de retomber sur le premier. */
    public static final String SHEET_MEASUREMENTS = "Mesures";

    /** L'onglet qui dit comment remplir le précédent. */
    static final String SHEET_HOWTO = "Mode d'emploi";

    /** En-tête de la colonne des rangs de pièce. */
    static final String HEADER_PIECE = "Pièce";

    /** Taille maximale admise pour un classeur versé (OWASP : une borne avant POI). */
    public static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;

    /**
     * Nombre de lignes et de colonnes parcourues au plus.
     *
     * <p>Une feuille peut déclarer un million de lignes vides : les parcourir pour
     * n'y rien trouver bloquerait la requête. La borne est large devant le nombre
     * de lignes retenues, pour qu'un relevé aéré de lignes vides passe quand même.
     *
     * <p>Le taux de décompression, lui, reste celui de POI : son défaut refuse déjà
     * les archives qui gonflent d'un facteur cent, et le modifier est un réglage
     * global que ce lecteur n'a pas à imposer au reste de l'application.
     */
    private static final int SCAN_LIMIT = 500;

    /** Le type MIME d'un classeur .xlsx, tel qu'il est produit et attendu. */
    public static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private SpcMeasurementsWorkbook() {}

    // ------------------------------------------------------------------
    // Le modèle à remplir
    // ------------------------------------------------------------------

    /**
     * Le classeur modèle : une ligne par pièce du prélèvement, une colonne par
     * sous-groupe, et un onglet qui dit quoi en faire.
     *
     * @param sampleSize    nombre de pièces mesurées à chaque prélèvement (lignes)
     * @param subgroupCount nombre de prélèvements composant l'étude (colonnes)
     * @throws SpcValidationException si l'une des deux dimensions sort des bornes
     */
    public static byte[] template(int sampleSize, int subgroupCount) {
        if (sampleSize < ChartFactors.MIN_SUBGROUP_SIZE || sampleSize > ChartFactors.MAX_SUBGROUP_SIZE) {
            throw new SpcValidationException("sampleSize", "La taille d'échantillon va de "
                    + ChartFactors.MIN_SUBGROUP_SIZE + " à " + ChartFactors.MAX_SUBGROUP_SIZE + ".");
        }
        if (subgroupCount < CapabilityCalculation.MIN_SUBGROUPS
                || subgroupCount > CapabilityCalculation.MAX_SUBGROUPS) {
            throw new SpcValidationException("subgroupCount", "Le nombre de sous-groupes va de "
                    + CapabilityCalculation.MIN_SUBGROUPS + " à "
                    + CapabilityCalculation.MAX_SUBGROUPS + ".");
        }
        try (Workbook classeur = new XSSFWorkbook();
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            remplirModele(classeur, sampleSize, subgroupCount);
            modeEmploi(classeur, sampleSize, subgroupCount);
            classeur.write(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Écriture du modèle de mesures SPC", e);
        }
    }

    private static void remplirModele(Workbook classeur, int sampleSize, int subgroupCount) {
        Sheet feuille = classeur.createSheet(SHEET_MEASUREMENTS);
        CellStyle entete = styleEntete(classeur);
        CellStyle rang = styleRang(classeur);

        Row titres = feuille.createRow(0);
        cellule(titres, 0, HEADER_PIECE, entete);
        for (int c = 0; c < subgroupCount; c++) {
            cellule(titres, c + 1, "Sous-groupe " + (c + 1), entete);
        }
        for (int r = 0; r < sampleSize; r++) {
            Row ligne = feuille.createRow(r + 1);
            Cell numero = ligne.createCell(0);
            numero.setCellValue(r + 1);
            numero.setCellStyle(rang);
        }
        // En-têtes et rangs figés : sans eux, on perd la colonne qu'on remplit
        // dès le dixième sous-groupe.
        feuille.createFreezePane(1, 1);
        feuille.setColumnWidth(0, 2200);
        for (int c = 0; c < subgroupCount; c++) {
            feuille.setColumnWidth(c + 1, 3600);
        }
    }

    private static void modeEmploi(Workbook classeur, int sampleSize, int subgroupCount) {
        Sheet feuille = classeur.createSheet(SHEET_HOWTO);
        CellStyle gras = styleGras(classeur);
        List<String> lignes = List.of(
                "Comment remplir ce classeur",
                "",
                "1. Une LIGNE par pièce mesurée dans un prélèvement : ce classeur en compte "
                        + sampleSize + ".",
                "2. Une COLONNE par prélèvement (sous-groupe) : ce classeur en compte "
                        + subgroupCount + ".",
                "3. N'écrivez que des nombres dans la grille. La colonne « " + HEADER_PIECE
                        + " » et la ligne d'en-tête sont là pour vous repérer ; elles sont ignorées à l'import.",
                "4. Le séparateur décimal peut être la virgule ou le point.",
                "5. Une cellule vide reste vide : la pièce n'a pas été mesurée, elle ne compte pas pour zéro.",
                "6. N'ajoutez ni ligne de total, ni moyenne, ni étendue : la plateforme les recalcule.",
                "7. Vous pouvez retirer des colonnes de droite si l'étude compte moins de sous-groupes.",
                "",
                "Limites : de " + ChartFactors.MIN_SUBGROUP_SIZE + " à " + ChartFactors.MAX_SUBGROUP_SIZE
                        + " lignes, de " + CapabilityCalculation.MIN_SUBGROUPS + " à "
                        + CapabilityCalculation.MAX_SUBGROUPS + " colonnes.",
                "Au-delà, l'import conserve ce qui tient dans ces bornes et le signale.");
        for (int i = 0; i < lignes.size(); i++) {
            Row ligne = feuille.createRow(i);
            cellule(ligne, 0, lignes.get(i), i == 0 ? gras : null);
        }
        feuille.setColumnWidth(0, 28000);
    }

    // ------------------------------------------------------------------
    // La lecture d'un classeur rempli
    // ------------------------------------------------------------------

    /**
     * Ce qu'un classeur versé a donné : une grille de texte, ses dimensions, et ce
     * qui a été écarté.
     *
     * <p>La grille reste du TEXTE : le navigateur l'affiche telle quelle dans la
     * même grille que la saisie à la main, et c'est l'enregistrement de l'étude —
     * et lui seul — qui convertit et recalcule.
     *
     * @param sampleSize    nombre de lignes retenues
     * @param subgroupCount nombre de colonnes retenues
     * @param rows          les valeurs, ligne par ligne, chaîne vide pour une cellule vide
     * @param warnings      ce que la lecture a écarté ou corrigé, en clair
     */
    public record ImportedGrid(int sampleSize, int subgroupCount,
                               List<List<String>> rows, List<String> warnings) {}

    /**
     * Lit la grille d'un classeur .xlsx.
     *
     * <p><b>Ce que la lecture tolère.</b> La ligne d'en-tête est retirée si aucune
     * de ses cellules non vides n'est un nombre ; la première colonne est
     * retirée si aucune de ses cellules n'est un nombre, ou si elles valent toutes
     * leur propre rang (la colonne « {@value #HEADER_PIECE} » du modèle). Tout le
     * reste est pris pour des mesures : deviner davantage, c'est se tromper sur le
     * fichier de quelqu'un d'autre.
     *
     * @throws SpcValidationException si les octets ne sont pas un classeur lisible,
     *                                ou si la grille est vide
     */
    public static ImportedGrid read(byte[] content) {
        if (content == null || content.length == 0) {
            throw new SpcValidationException("file", "Le fichier est vide.");
        }
        if (content.length > MAX_UPLOAD_BYTES) {
            throw new SpcValidationException("file", "Le fichier dépasse "
                    + (MAX_UPLOAD_BYTES / (1024 * 1024)) + " Mo.");
        }
        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            return lire(feuilleDesMesures(classeur));
        } catch (IOException | RuntimeException e) {
            if (e instanceof SpcValidationException refus) {
                throw refus;
            }
            throw new SpcValidationException("file", "Fichier illisible : attendu un classeur Excel (.xlsx).");
        }
    }

    private static Sheet feuilleDesMesures(Workbook classeur) {
        for (int i = 0; i < classeur.getNumberOfSheets(); i++) {
            Sheet feuille = classeur.getSheetAt(i);
            if (SHEET_MEASUREMENTS.equalsIgnoreCase(feuille.getSheetName().trim())) {
                return feuille;
            }
        }
        if (classeur.getNumberOfSheets() == 0) {
            throw new SpcValidationException("file", "Le classeur ne contient aucune feuille.");
        }
        return classeur.getSheetAt(0);
    }

    private static ImportedGrid lire(Sheet feuille) {
        List<List<String>> brut = new ArrayList<>();
        int dernier = Math.min(feuille.getLastRowNum(), SCAN_LIMIT - 1);
        for (int r = 0; r <= dernier; r++) {
            brut.add(valeurs(feuille.getRow(r)));
        }
        // Les lignes entièrement vides ne portent rien : une ligne de respiration
        // au milieu d'un relevé ne doit pas devenir une pièce non mesurée.
        brut.removeIf(ligne -> ligne.stream().allMatch(String::isEmpty));
        if (brut.isEmpty()) {
            throw new SpcValidationException("file", "Aucune mesure trouvée dans le classeur.");
        }

        List<String> avertissements = new ArrayList<>();
        if (enTete(brut.get(0))) {
            brut.remove(0);
            if (brut.isEmpty()) {
                throw new SpcValidationException("file", "Le classeur ne porte qu'une ligne d'en-tête.");
            }
        }
        if (colonneDeRangs(brut)) {
            brut = brut.stream().map(ligne -> ligne.subList(1, ligne.size())).toList();
        }

        int largeur = brut.stream().mapToInt(List::size).max().orElse(0);
        if (largeur == 0) {
            throw new SpcValidationException("file", "Aucune mesure trouvée dans le classeur.");
        }
        int lignes = brut.size();
        if (lignes > ChartFactors.MAX_SUBGROUP_SIZE) {
            avertissements.add(lignes + " lignes lues : seules les "
                    + ChartFactors.MAX_SUBGROUP_SIZE + " premières sont conservées.");
            lignes = ChartFactors.MAX_SUBGROUP_SIZE;
        }
        if (largeur > CapabilityCalculation.MAX_SUBGROUPS) {
            avertissements.add(largeur + " colonnes lues : seules les "
                    + CapabilityCalculation.MAX_SUBGROUPS + " premières sont conservées.");
            largeur = CapabilityCalculation.MAX_SUBGROUPS;
        }

        final int colonnes = largeur;
        List<List<String>> grille = new ArrayList<>();
        int nonNumeriques = 0;
        for (int r = 0; r < lignes; r++) {
            List<String> source = brut.get(r);
            List<String> ligne = new ArrayList<>(colonnes);
            for (int c = 0; c < colonnes; c++) {
                String valeur = c < source.size() ? source.get(c) : "";
                if (!valeur.isEmpty() && !numerique(valeur)) {
                    nonNumeriques++;
                    valeur = "";
                }
                ligne.add(valeur);
            }
            grille.add(ligne);
        }
        if (nonNumeriques > 0) {
            avertissements.add(nonNumeriques
                    + " cellule(s) non numérique(s) ignorée(s) : seuls des nombres sont des mesures.");
        }
        if (grille.stream().allMatch(ligne -> ligne.stream().allMatch(String::isEmpty))) {
            throw new SpcValidationException("file", "Aucune mesure numérique trouvée dans le classeur.");
        }
        return new ImportedGrid(lignes, colonnes, grille, List.copyOf(avertissements));
    }

    /**
     * Une ligne d'en-tête : aucune de ses cellules non vides n'est un nombre.
     *
     * <p>« Sa première cellule n'est pas un nombre » ne suffisait pas : une grille
     * dont la colonne de gauche porte un libellé de pièce (« Pièce A », puis les
     * mesures) voyait CHACUNE de ses lignes prise pour un en-tête, et la première
     * disparaissait. Un en-tête ne porte aucune mesure, par définition.
     */
    private static boolean enTete(List<String> ligne) {
        List<String> remplies = ligne.stream().filter(v -> !v.isEmpty()).toList();
        return !remplies.isEmpty() && remplies.stream().noneMatch(SpcMeasurementsWorkbook::numerique);
    }

    /**
     * La première colonne ne porte que des rangs de pièce.
     *
     * <p>Soit elle ne contient aucun nombre, soit chacune de ses valeurs égale son
     * propre rang (1, 2, 3…). Un relevé dont la première colonne serait, par
     * hasard, 1, 2, 3… serait pris pour des rangs : le cas est assumé, et il est
     * bien plus rare que le modèle, qui porte cette colonne par construction.
     */
    private static boolean colonneDeRangs(List<List<String>> lignes) {
        boolean unNombre = false;
        boolean tousDesRangs = true;
        for (int r = 0; r < lignes.size(); r++) {
            List<String> ligne = lignes.get(r);
            if (ligne.isEmpty()) {
                continue;
            }
            String tete = ligne.get(0);
            if (tete.isEmpty()) {
                tousDesRangs = false;
                continue;
            }
            if (numerique(tete)) {
                unNombre = true;
                if (!tete.equals(String.valueOf(r + 1))) {
                    tousDesRangs = false;
                }
            }
        }
        return !unNombre || tousDesRangs;
    }

    /** Le texte d'une cellule, nombre compris, sans notation scientifique. */
    private static List<String> valeurs(Row ligne) {
        List<String> valeurs = new ArrayList<>();
        if (ligne == null) {
            return valeurs;
        }
        int derniere = Math.min(ligne.getLastCellNum(), SCAN_LIMIT);
        for (int c = 0; c < derniere; c++) {
            valeurs.add(texte(ligne.getCell(c)));
        }
        return valeurs;
    }

    private static String texte(Cell cellule) {
        if (cellule == null) {
            return "";
        }
        CellType type = cellule.getCellType() == CellType.FORMULA
                ? cellule.getCachedFormulaResultType()
                : cellule.getCellType();
        return switch (type) {
            case NUMERIC -> nombre(cellule.getNumericCellValue());
            case STRING -> cellule.getStringCellValue().trim();
            case BOOLEAN -> String.valueOf(cellule.getBooleanCellValue());
            default -> "";
        };
    }

    /**
     * Un nombre tel qu'on l'affiche dans la grille : sans exposant, et sans les
     * zéros que le flottant traîne derrière lui (49,000000000000004).
     */
    private static String nombre(double valeur) {
        BigDecimal arrondi = BigDecimal.valueOf(valeur).setScale(6, RoundingMode.HALF_UP);
        return arrondi.stripTrailingZeros().toPlainString();
    }

    /** Un nombre, virgule ou point décimal, avec un signe facultatif. */
    static boolean numerique(String valeur) {
        if (valeur == null) {
            return false;
        }
        String net = valeur.trim().replace(',', '.').replace(" ", "").replace(" ", "");
        if (net.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(net);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Styles
    // ------------------------------------------------------------------

    private static CellStyle styleEntete(Workbook classeur) {
        CellStyle style = classeur.createCellStyle();
        Font police = classeur.createFont();
        police.setBold(true);
        style.setFont(police);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        bordures(style);
        return style;
    }

    private static CellStyle styleRang(Workbook classeur) {
        CellStyle style = classeur.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        bordures(style);
        return style;
    }

    private static CellStyle styleGras(Workbook classeur) {
        CellStyle style = classeur.createCellStyle();
        Font police = classeur.createFont();
        police.setBold(true);
        style.setFont(police);
        return style;
    }

    private static void bordures(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static void cellule(Row ligne, int colonne, String valeur, CellStyle style) {
        Cell cellule = ligne.createCell(colonne);
        cellule.setCellValue(valeur);
        if (style != null) {
            cellule.setCellStyle(style);
        }
    }
}
