package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import com.openlab.qualitos.quality.change.domain.ImpactLevel;
import com.openlab.qualitos.quality.change.domain.MocClassification;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Le classeur Excel des demandes : colonnes, libellés, décisions, et pas de formule. */
class MocWorkbookTest {

    @Test
    void uneLigneParDemandeAvecSesDomainesEtSesDecisions() throws Exception {
        ChangeRequest c = new ChangeRequest();
        c.setId(UUID.randomUUID());
        c.setCode("MOC-2026-014");
        c.setTitle("=HYPERLINK(\"http://x\")");
        c.setType(ChangeRequestType.TEMPORARY);
        c.setSite("Atelier 2");
        c.setRequesterName("S. Benali");
        c.setCreatedAt(Instant.parse("2026-09-20T08:00:00Z"));
        c.setStatus(ChangeRequestStatus.IN_VALIDATION);
        c.setClassification(MocClassification.CRITICAL);
        c.setImpactScore(54);
        Map<ImpactDomain, ImpactLevel> niveaux = new EnumMap<>(ImpactDomain.class);
        niveaux.put(ImpactDomain.SAFETY, ImpactLevel.MEDIUM);
        niveaux.put(ImpactDomain.PRODUCTION, ImpactLevel.HIGH);

        ChangeValidation approuvee = carte("HSE", ApprovalDecision.APPROVED, "J. Dupont", "OK",
                Instant.parse("2026-09-28T07:12:00Z"));
        ChangeValidation attente = carte("Directeur de site", ApprovalDecision.PENDING, null, null, null);

        ChangeRequest brouillon = new ChangeRequest();
        brouillon.setCode("MOC-2026-015");
        brouillon.setTitle("Brouillon");
        brouillon.setType(ChangeRequestType.PERMANENT);
        brouillon.setStatus(ChangeRequestStatus.DRAFT);
        brouillon.setClassification(MocClassification.NOT_ASSESSED);

        byte[] xlsx = MocWorkbook.build(List.of(new MocWorkbook.Row(c, niveaux, List.of(approuvee, attente)),
                new MocWorkbook.Row(brouillon, Map.of(), List.of())));

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet s = wb.getSheet(MocWorkbook.SHEET);
            Row head = s.getRow(0);
            assertThat(head.getCell(0).getStringCellValue()).isEqualTo("Code");
            assertThat(head.getLastCellNum()).isEqualTo((short) MocWorkbook.HEADERS.size());
            assertThat(MocWorkbook.HEADERS).contains("Sécurité", "Formation", "Décisions", "Score", "Classification");

            Row r = s.getRow(1);
            assertThat(r.getCell(0).getStringCellValue()).isEqualTo("MOC-2026-014");
            // Un titre qui ressemble à une formule reste du texte.
            assertThat(r.getCell(1).getCellType()).isEqualTo(CellType.STRING);
            assertThat(r.getCell(1).getStringCellValue()).startsWith("=HYPERLINK");
            assertThat(r.getCell(2).getStringCellValue()).isEqualTo("Temporaire");
            assertThat(r.getCell(4).getStringCellValue()).isEqualTo("S. Benali");
            assertThat(r.getCell(5).getStringCellValue()).isEqualTo("20/09/2026");
            assertThat(r.getCell(6).getStringCellValue()).isEqualTo("En validation");
            assertThat(r.getCell(7).getStringCellValue()).isEqualTo("Critique");
            assertThat(r.getCell(8).getNumericCellValue()).isEqualTo(54.0);
            assertThat(r.getCell(9).getStringCellValue()).isEqualTo("Moyen");
            assertThat(r.getCell(12).getStringCellValue()).isEqualTo("Élevé");
            assertThat(r.getCell(10).getStringCellValue()).isEmpty();
            String decisions = r.getCell(17).getStringCellValue();
            assertThat(decisions).contains("HSE : approuvé par J. Dupont le 28/09/2026 09:12 — OK");
            assertThat(decisions).contains("Directeur de site : en attente");

            Row b = s.getRow(2);
            assertThat(b.getCell(2).getStringCellValue()).isEqualTo("Permanent");
            assertThat(b.getCell(6).getStringCellValue()).isEqualTo("Brouillon");
            assertThat(b.getCell(7).getStringCellValue()).isEqualTo("Non évalué");
            assertThat(b.getCell(8).getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    void unClasseurSansDemandeGardeSesEnTetes() throws Exception {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(MocWorkbook.build(List.of())))) {
            assertThat(wb.getSheet(MocWorkbook.SHEET).getRow(0).getCell(0).getStringCellValue()).isEqualTo("Code");
        }
    }

    @Test
    void lesLibellesCouvrentToutesLesValeurs() {
        for (ChangeRequestStatus st : ChangeRequestStatus.values()) assertThat(MocWorkbook.statusLabel(st)).isNotBlank();
        for (MocClassification cl : MocClassification.values()) assertThat(MocWorkbook.classificationLabel(cl)).isNotBlank();
        for (ImpactLevel l : ImpactLevel.values()) assertThat(MocWorkbook.levelLabel(l)).isNotBlank();
        assertThat(MocWorkbook.statusLabel(ChangeRequestStatus.APPROVED)).isEqualTo("Approuvé");
        assertThat(MocWorkbook.statusLabel(ChangeRequestStatus.REJECTED)).isEqualTo("Rejeté");
        assertThat(MocWorkbook.statusLabel(ChangeRequestStatus.IN_ANALYSIS)).isEqualTo("En analyse");
        assertThat(MocWorkbook.classificationLabel(MocClassification.MINOR)).isEqualTo("Mineur");
        assertThat(MocWorkbook.classificationLabel(MocClassification.MAJOR)).isEqualTo("Majeur");
        assertThat(MocWorkbook.levelLabel(ImpactLevel.NONE)).isEqualTo("Aucun");
        assertThat(MocWorkbook.levelLabel(ImpactLevel.LOW)).isEqualTo("Faible");
        assertThat(MocWorkbook.decision(carte("X", ApprovalDecision.REJECTED, null, "Non", null)))
                .isEqualTo("X : rejeté — Non");
    }

    static ChangeValidation carte(String role, ApprovalDecision d, String qui, String commentaire, Instant quand) {
        ChangeValidation v = new ChangeValidation();
        v.setRoleLabel(role);
        v.setDecision(d);
        v.setDecidedByName(qui);
        v.setComment(commentaire);
        v.setDecidedAt(quand);
        return v;
    }
}
