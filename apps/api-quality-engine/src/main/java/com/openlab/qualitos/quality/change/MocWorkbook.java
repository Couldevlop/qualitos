package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import com.openlab.qualitos.quality.change.domain.ImpactLevel;
import com.openlab.qualitos.quality.change.domain.MocClassification;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * L'extraction Excel de toutes les demandes de changement d'un client.
 *
 * <p>Un vrai .xlsx (même brique que l'export produit) : une ligne par demande,
 * les huit domaines en colonnes, les décisions en clair. En-têtes figés et
 * auto-filtre : on extrait pour trier et filtrer.
 *
 * <p>Les cellules sont écrites comme TEXTE, jamais comme formule : un titre qui
 * commence par « = » reste un titre (pas d'injection de formule).
 *
 * <p>Ne lit aucune base : on lui donne des lignes déjà chargées, ce qui le rend
 * testable sans contexte Spring.
 */
public final class MocWorkbook {

    static final String SHEET = "Demandes de changement";
    static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE);

    static final List<String> HEADERS = headers();

    /** Une demande, son analyse et ses cartes. */
    public record Row(ChangeRequest request, Map<ImpactDomain, ImpactLevel> levels, List<ChangeValidation> validations) {}

    private MocWorkbook() {}

    private static List<String> headers() {
        List<String> h = new ArrayList<>(List.of("Code", "Titre", "Type", "Site / unité", "Demandeur",
                "Date de la demande", "Statut", "Classification", "Score"));
        for (ImpactDomain d : ImpactDomain.values()) {
            h.add(domainLabel(d));
        }
        h.add("Décisions");
        return List.copyOf(h);
    }

    public static byte[] build(List<Row> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(workbook);
            CellStyle body = bodyStyle(workbook);
            Sheet sheet = workbook.createSheet(SHEET);
            org.apache.poi.ss.usermodel.Row head = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(HEADERS.get(i));
                cell.setCellStyle(header);
            }
            int r = 1;
            for (Row row : rows) {
                write(sheet.createRow(r++), row, body);
            }
            if (r > 1) {
                sheet.setAutoFilter(new CellRangeAddress(0, r - 1, 0, HEADERS.size() - 1));
            }
            sheet.createFreezePane(0, 1);
            for (int i = 0; i < HEADERS.size(); i++) {
                sheet.autoSizeColumn(i);
                int w = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(Math.max(w + 512, 2600), 14000));
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("export Excel des demandes de changement impossible", e);
        }
    }

    private static void write(org.apache.poi.ss.usermodel.Row x, Row row, CellStyle style) {
        ChangeRequest c = row.request();
        int col = 0;
        text(x, col++, style, c.getCode());
        text(x, col++, style, c.getTitle());
        text(x, col++, style, c.getType() == ChangeRequestType.TEMPORARY ? "Temporaire" : "Permanent");
        text(x, col++, style, c.getSite());
        text(x, col++, style, c.getRequesterName());
        text(x, col++, style, c.getCreatedAt() == null ? null : DATE.format(c.getCreatedAt().atZone(ZONE)));
        text(x, col++, style, statusLabel(c.getStatus()));
        text(x, col++, style, classificationLabel(c.getClassification()));
        Cell score = x.createCell(col++);
        if (c.getImpactScore() != null) {
            score.setCellValue(c.getImpactScore());
        }
        score.setCellStyle(style);
        for (ImpactDomain d : ImpactDomain.values()) {
            ImpactLevel l = row.levels().get(d);
            text(x, col++, style, l == null ? null : levelLabel(l));
        }
        text(x, col, style, row.validations().stream().map(MocWorkbook::decision)
                .collect(Collectors.joining("\n")));
    }

    static String decision(ChangeValidation v) {
        StringBuilder sb = new StringBuilder(v.getRoleLabel()).append(" : ");
        switch (v.getDecision()) {
            case PENDING -> sb.append("en attente");
            case APPROVED -> sb.append("approuvé");
            case REJECTED -> sb.append("rejeté");
        }
        if (v.getDecidedByName() != null) {
            sb.append(" par ").append(v.getDecidedByName());
        }
        if (v.getDecidedAt() != null) {
            sb.append(" le ").append(dateTime(v.getDecidedAt()));
        }
        if (v.getComment() != null) {
            sb.append(" — ").append(v.getComment());
        }
        return sb.toString();
    }

    static String dateTime(Instant i) {
        return DATE_TIME.format(i.atZone(ZONE));
    }

    static String statusLabel(ChangeRequestStatus s) {
        return switch (s) {
            case DRAFT -> "Brouillon";
            case IN_ANALYSIS -> "En analyse";
            case IN_VALIDATION -> "En validation";
            case APPROVED -> "Approuvé";
            case REJECTED -> "Rejeté";
        };
    }

    static String classificationLabel(MocClassification c) {
        return switch (c) {
            case NOT_ASSESSED -> "Non évalué";
            case MINOR -> "Mineur";
            case MAJOR -> "Majeur";
            case CRITICAL -> "Critique";
        };
    }

    static String levelLabel(ImpactLevel l) {
        return switch (l) {
            case NONE -> "Aucun";
            case LOW -> "Faible";
            case MEDIUM -> "Moyen";
            case HIGH -> "Élevé";
        };
    }

    static String domainLabel(ImpactDomain d) {
        return switch (d) {
            case SAFETY -> "Sécurité";
            case HEALTH_ENVIRONMENT -> "Santé et environnement";
            case QUALITY -> "Qualité";
            case PRODUCTION -> "Production";
            case MAINTENANCE -> "Maintenance";
            case COSTS -> "Coûts";
            case REGULATORY -> "Réglementaire";
            case TRAINING -> "Formation";
        };
    }

    /** Une chaîne vide plutôt que {@code null} ; toujours du texte, jamais une formule. */
    private static void text(org.apache.poi.ss.usermodel.Row row, int col, CellStyle style, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static CellStyle headerStyle(Workbook wb) {
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        CellStyle s = wb.createCellStyle();
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        border(s);
        return s;
    }

    private static CellStyle bodyStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setVerticalAlignment(VerticalAlignment.TOP);
        s.setWrapText(true);
        border(s);
        return s;
    }

    private static void border(CellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}
