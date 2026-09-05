package com.openlab.qualitos.quality.product.export;

import com.openlab.qualitos.quality.controlplan.application.ControlPlanDto;
import com.openlab.qualitos.quality.risk.FmeaDto;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Le classeur Excel d'un produit : une feuille PFMEA, une feuille plan de
 * surveillance.
 *
 * <p><b>Un vrai .xlsx, pas un CSV renommé.</b> Les deux tableaux ne tiennent
 * pas dans un seul onglet — ils n'ont ni les mêmes colonnes ni le même nombre
 * de lignes — et le plan de surveillance en compte à lui seul une vingtaine.
 * Un CSV obligerait à produire deux fichiers, à les nommer, et à expliquer
 * lequel ouvrir en premier.
 *
 * <p><b>Ce que le classeur porte, et pourquoi.</b> Il reprend ce que les écrans
 * montrent, plus ce qu'ils cachent faute de place : le RPN <b>après action</b>,
 * la cause de défaillance, les contrôles en place, le responsable et l'échéance
 * de l'action. C'est précisément ce qu'on vient chercher dans un tableur — on
 * n'exporte pas pour relire, on exporte pour trier, filtrer et transmettre à
 * quelqu'un qui n'a pas la plateforme.
 *
 * <p><b>Les en-têtes sont figés et un auto-filtre est posé</b> : sans eux, la
 * première chose que fait le destinataire est de les remettre à la main.
 *
 * <p>Cette classe ne lit AUCUNE base : on lui donne des vues déjà chargées.
 * C'est ce qui permet de la tester sans contexte Spring, sur des données
 * choisies — un classeur juste sur des données inventées reste un classeur
 * juste.
 */
public final class ProductWorkbook {

    /** Nom des onglets — court, sans caractère interdit par Excel (: \ / ? * [ ]). */
    static final String SHEET_PFMEA = "PFMEA";
    static final String SHEET_CONTROL_PLAN = "Plan de surveillance";

    static final List<String> PFMEA_HEADERS = List.of(
            "Rang", "Fonction", "Mode de défaillance", "Effet", "Cause",
            "Contrôles actuels", "S", "O", "D", "RPN",
            "Action recommandée", "Responsable", "Échéance", "Actions menées",
            "S après", "O après", "D après", "RPN après");

    static final List<String> CONTROL_PLAN_HEADERS = List.of(
            "Rang", "Machine", "N° caractéristique", "Caractéristique",
            "Caractéristique spécifiée", "Type", "Classe spéciale", "Spécification",
            "Tolérance min.", "Tolérance max.", "Unité", "Technique de mesure",
            "Taille d'échantillon", "Fréquence", "Méthode de contrôle",
            "Plan de réaction", "Référence mode opératoire", "Entrée/Sortie",
            "Qui mesure", "Lieu d'enregistrement");

    private ProductWorkbook() {}

    /**
     * Construit le classeur.
     *
     * @param productLabel   ce qui identifie le produit pour un lecteur humain
     * @param pfmea          les lignes d'analyse, dans l'ordre où l'écran les montre
     * @param controlPlan    le plan de surveillance et ses lignes, ou {@code null}
     *                       quand le produit n'en a pas — ou que le module est
     *                       fermé pour ce client
     */
    public static byte[] build(String productLabel,
                               List<FmeaDto.ItemResponse> pfmea,
                               ControlPlanDto.Detail controlPlan) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Styles styles = new Styles(workbook);
            writePfmea(workbook, styles, productLabel, pfmea);
            writeControlPlan(workbook, styles, productLabel, controlPlan);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            // Un classeur qu'on ne sait pas écrire n'est pas un classeur vide :
            // c'est une panne. Rendre zéro octet enverrait au client un fichier
            // qu'Excel refuse d'ouvrir, sans dire pourquoi.
            throw new UncheckedIOException("export Excel du produit impossible", e);
        }
    }

    // ---------- PFMEA ----------

    private static void writePfmea(Workbook workbook, Styles styles,
                                   String productLabel, List<FmeaDto.ItemResponse> items) {
        Sheet sheet = workbook.createSheet(SHEET_PFMEA);
        int rowIndex = writeTitle(sheet, styles, productLabel, SHEET_PFMEA, PFMEA_HEADERS.size());
        rowIndex = writeHeaders(sheet, styles, rowIndex, PFMEA_HEADERS);

        for (FmeaDto.ItemResponse item : items) {
            Row row = sheet.createRow(rowIndex++);
            int col = 0;
            number(row, col++, styles, item.sequenceNo());
            text(row, col++, styles, item.function());
            text(row, col++, styles, item.failureMode());
            text(row, col++, styles, item.failureEffect());
            text(row, col++, styles, item.failureCause());
            text(row, col++, styles, item.currentControls());
            number(row, col++, styles, item.severity());
            number(row, col++, styles, item.occurrence());
            number(row, col++, styles, item.detection());
            // Le RPN en GRAS : c'est la colonne sur laquelle le destinataire va
            // trier, et celle qui décide de l'ordre de traitement.
            Cell rpn = row.createCell(col++);
            rpn.setCellValue(item.rpn());
            rpn.setCellStyle(styles.numberStrong);
            text(row, col++, styles, item.recommendedAction());
            text(row, col++, styles, item.actionOwnerName());
            text(row, col++, styles, item.actionDueDate() == null ? null : item.actionDueDate().toString());
            text(row, col++, styles, item.actionsTaken());
            number(row, col++, styles, item.resultingSeverity());
            number(row, col++, styles, item.resultingOccurrence());
            number(row, col++, styles, item.resultingDetection());
            number(row, col, styles, item.rpnAfter());
        }

        finish(sheet, rowIndex, PFMEA_HEADERS.size());
    }

    // ---------- Plan de surveillance ----------

    private static void writeControlPlan(Workbook workbook, Styles styles,
                                         String productLabel, ControlPlanDto.Detail plan) {
        Sheet sheet = workbook.createSheet(SHEET_CONTROL_PLAN);
        String subtitle = plan == null
                ? SHEET_CONTROL_PLAN
                : SHEET_CONTROL_PLAN + " — " + plan.plan().code()
                        + " (révision " + plan.plan().revision()
                        + ", " + plan.plan().status() + ")";
        int rowIndex = writeTitle(sheet, styles, productLabel, subtitle, CONTROL_PLAN_HEADERS.size());
        rowIndex = writeHeaders(sheet, styles, rowIndex, CONTROL_PLAN_HEADERS);

        List<ControlPlanDto.LineView> lines = plan == null ? List.of() : plan.lines();
        for (ControlPlanDto.LineView line : lines) {
            Row row = sheet.createRow(rowIndex++);
            int col = 0;
            number(row, col++, styles, line.sequenceNo());
            text(row, col++, styles, line.machine());
            text(row, col++, styles, line.characteristicNo());
            text(row, col++, styles, line.characteristicLabel());
            text(row, col++, styles, line.specifiedCharacteristic());
            text(row, col++, styles, line.characteristicType() == null ? null : line.characteristicType().name());
            text(row, col++, styles, line.specialClass() == null ? null : line.specialClass().name());
            text(row, col++, styles, line.specification());
            decimal(row, col++, styles, line.toleranceLower());
            decimal(row, col++, styles, line.toleranceUpper());
            text(row, col++, styles, line.unit());
            text(row, col++, styles, line.measurementTechnique());
            text(row, col++, styles, line.sampleSize());
            text(row, col++, styles, line.sampleFrequency());
            text(row, col++, styles, line.controlMethod());
            text(row, col++, styles, line.reactionPlan());
            text(row, col++, styles, line.sopReference());
            text(row, col++, styles, line.inputOutput() == null ? null : line.inputOutput().name());
            text(row, col++, styles, line.whoMeasures());
            text(row, col, styles, line.recordingLocation());
        }

        finish(sheet, rowIndex, CONTROL_PLAN_HEADERS.size());
    }

    // ---------- mise en page ----------

    /**
     * Titre de la feuille : le produit, puis ce que la feuille contient.
     *
     * <p>Il est SUR la feuille et pas seulement dans le nom du fichier : un
     * onglet recopié dans un autre classeur perd son fichier d'origine, et une
     * feuille de cotation qu'on ne sait plus rattacher à un produit ne vaut
     * rien.
     */
    private static int writeTitle(Sheet sheet, Styles styles,
                                  String productLabel, String subtitle, int width) {
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue(productLabel + " — " + subtitle);
        cell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, Math.max(1, width) - 1));
        return 2;   // une ligne vide sépare le titre du tableau
    }

    private static int writeHeaders(Sheet sheet, Styles styles, int rowIndex, List<String> headers) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(styles.header);
        }
        return rowIndex + 1;
    }

    /**
     * Auto-filtre, volets figés et largeurs.
     *
     * <p>Sans eux, la première chose que fait le destinataire est de les poser
     * à la main : un tableau de vingt colonnes sans en-tête figé se lit en
     * remontant sans cesse pour savoir dans quelle colonne on est.
     */
    private static void finish(Sheet sheet, int lastRowExclusive, int width) {
        int headerRow = 2;
        if (lastRowExclusive > headerRow + 1) {
            sheet.setAutoFilter(new CellRangeAddress(headerRow, lastRowExclusive - 1, 0, width - 1));
        }
        sheet.createFreezePane(0, headerRow + 1);
        for (int i = 0; i < width; i++) {
            sheet.autoSizeColumn(i);
            // autoSizeColumn suit le contenu : une cause de défaillance de trois
            // lignes produirait une colonne large de tout l'écran. On borne.
            int current = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(Math.max(current + 512, 2200), 12000));
        }
    }

    // ---------- écriture de cellule ----------

    /** Une chaîne vide plutôt que `null` : Excel affiche « null » sinon. */
    private static void text(Row row, int col, Styles styles, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(styles.body);
    }

    private static void number(Row row, int col, Styles styles, Integer value) {
        Cell cell = row.createCell(col);
        // Une cote NON renseignée reste VIDE : y écrire 0 ferait croire à une
        // cotation faite, et un zéro sur une échelle qui commence à 1 n'existe
        // pas. La différence se voit à la somme comme au tri.
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(styles.number);
    }

    private static void decimal(Row row, int col, Styles styles, BigDecimal value) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(styles.number);
    }

    /** Les styles, créés UNE fois : Excel plafonne le nombre de styles par classeur. */
    private static final class Styles {
        private final CellStyle title;
        private final CellStyle header;
        private final CellStyle body;
        private final CellStyle number;
        private final CellStyle numberStrong;

        Styles(Workbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font strongFont = workbook.createFont();
            strongFont.setBold(true);

            this.title = workbook.createCellStyle();
            this.title.setFont(titleFont);
            this.title.setVerticalAlignment(VerticalAlignment.CENTER);

            this.header = workbook.createCellStyle();
            this.header.setFont(headerFont);
            // Indigo QualitOS, le même que le PDF : une extraction reconnaissable
            // sans avoir à lire son en-tête.
            this.header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            this.header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            this.header.setAlignment(HorizontalAlignment.CENTER);
            this.header.setVerticalAlignment(VerticalAlignment.CENTER);
            this.header.setWrapText(true);
            border(this.header);

            this.body = workbook.createCellStyle();
            this.body.setVerticalAlignment(VerticalAlignment.TOP);
            this.body.setWrapText(true);
            border(this.body);

            this.number = workbook.createCellStyle();
            this.number.setAlignment(HorizontalAlignment.CENTER);
            this.number.setVerticalAlignment(VerticalAlignment.TOP);
            border(this.number);

            this.numberStrong = workbook.createCellStyle();
            this.numberStrong.cloneStyleFrom(this.number);
            this.numberStrong.setFont(strongFont);
        }

        private static void border(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
            style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
            style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
            style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        }
    }
}
