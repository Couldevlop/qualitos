package com.openlab.qualitos.quality.standards.auditblanc;

import com.openlab.qualitos.quality.standards.auditblanc.domain.ClauseGapFinding;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditCriticality;
import com.openlab.qualitos.quality.standards.auditblanc.domain.RemediationAction;
import com.openlab.qualitos.quality.standards.auditblanc.domain.RemediationPlanner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Plan de remédiation actionnable (§8.4 onglet 7). */
class RemediationPlannerTest {

    private static ClauseGapFinding gap(String code, String title, MockAuditCriticality crit,
                                        int total, int covered) {
        double ratio = total == 0 ? 0d : (double) covered / total;
        return new ClauseGapFinding(code, title, crit, ratio, total, covered, "constat", List.of());
    }

    @Test
    void observationsAreExcluded_majorsFirst() {
        List<RemediationAction> plan = RemediationPlanner.plan(List.of(
                gap("4.1", "Contexte", MockAuditCriticality.OBSERVATION, 2, 2),
                gap("7.1", "Ressources", MockAuditCriticality.MINOR, 2, 0),
                gap("8.1", "Maîtrise", MockAuditCriticality.MAJOR, 4, 0)));

        assertThat(plan).hasSize(2);
        assertThat(plan.get(0).clauseCode()).isEqualTo("8.1");
        assertThat(plan.get(0).criticality()).isEqualTo(MockAuditCriticality.MAJOR);
        assertThat(plan.get(0).priority()).isEqualTo("high");
        assertThat(plan.get(1).clauseCode()).isEqualTo("7.1");
        assertThat(plan.get(1).priority()).isEqualTo("medium");
    }

    @Test
    void targetModule_partialCoverage_isPdca() {
        RemediationAction a = RemediationPlanner.plan(List.of(
                gap("8.5", "Production", MockAuditCriticality.MINOR, 4, 2))).get(0);
        assertThat(a.targetModule()).isEqualTo("PDCA");
        assertThat(a.action()).contains("compléter la couverture (2/4)");
    }

    @Test
    void targetModule_trainingClause_isTraining() {
        RemediationAction a = RemediationPlanner.plan(List.of(
                gap("7.2", "Compétence et formation", MockAuditCriticality.MAJOR, 3, 0))).get(0);
        assertThat(a.targetModule()).isEqualTo("TRAINING");
        assertThat(a.action()).startsWith("Lever la non-conformité majeure");
    }

    @Test
    void targetModule_auditClause_isAudit() {
        RemediationAction a = RemediationPlanner.plan(List.of(
                gap("9.2", "Audit interne", MockAuditCriticality.MAJOR, 2, 0))).get(0);
        assertThat(a.targetModule()).isEqualTo("AUDIT");
    }

    @Test
    void targetModule_default_isDocumentControl() {
        RemediationAction a = RemediationPlanner.plan(List.of(
                gap("7.5", "Informations documentées", MockAuditCriticality.MINOR, 2, 0))).get(0);
        assertThat(a.targetModule()).isEqualTo("DOCUMENT_CONTROL");
        assertThat(a.action()).contains("produire la preuve associée");
    }

    @Test
    void targetModule_trainingTitleAtStart_isTraining() {
        RemediationAction a = RemediationPlanner.plan(List.of(
                gap("7.2", "Formation du personnel", MockAuditCriticality.MINOR, 2, 0))).get(0);
        assertThat(a.targetModule()).isEqualTo("TRAINING");
    }

    @Test
    void targetModule_informationsTitle_isDocumentControl_notTraining() {
        // « informations » contient « formation » : ne doit PAS basculer en TRAINING.
        RemediationAction a = RemediationPlanner.plan(List.of(
                gap("7.5", "Informations documentées", MockAuditCriticality.MAJOR, 2, 0))).get(0);
        assertThat(a.targetModule()).isEqualTo("DOCUMENT_CONTROL");
    }

    @Test
    void targetModule_habilitationTitle_isTraining() {
        RemediationAction a = RemediationPlanner.plan(List.of(
                gap("7.2", "Habilitation des opérateurs", MockAuditCriticality.MINOR, 2, 0))).get(0);
        assertThat(a.targetModule()).isEqualTo("TRAINING");
    }

    @Test
    void minorMajor_describeBranches_andCoveredZeroVsPositive() {
        // Majeure + 0 preuve → "Lever ... majeure" + "produire la preuve".
        RemediationAction major = RemediationPlanner.plan(List.of(
                gap("8.1", "Maîtrise", MockAuditCriticality.MAJOR, 4, 0))).get(0);
        assertThat(major.action()).startsWith("Lever la non-conformité majeure")
                .contains("produire la preuve associée");
        // Mineure + couverture partielle → "Traiter ... mineure" + "compléter".
        RemediationAction minor = RemediationPlanner.plan(List.of(
                gap("7.1", "Ressources", MockAuditCriticality.MINOR, 4, 2))).get(0);
        assertThat(minor.action()).startsWith("Traiter la non-conformité mineure")
                .contains("compléter la couverture (2/4)");
    }

    @Test
    void targetModule_surveillanceKeyword_isAudit() {
        RemediationAction a = RemediationPlanner.plan(List.of(
                gap("9.1", "Surveillance et mesure", MockAuditCriticality.MINOR, 2, 0))).get(0);
        assertThat(a.targetModule()).isEqualTo("AUDIT");
    }

    @Test
    void targetModule_nullTitle_defaultsToDocumentControl() {
        // ClauseGapFinding n'impose pas de titre → branche null de lower().
        ClauseGapFinding g = new ClauseGapFinding("8.1", null, MockAuditCriticality.MAJOR,
                0d, 2, 0, "constat", List.of());
        assertThat(RemediationPlanner.plan(List.of(g)).get(0).targetModule())
                .isEqualTo("DOCUMENT_CONTROL");
    }

    @Test
    void emptyGaps_yieldEmptyPlan() {
        assertThat(RemediationPlanner.plan(List.of())).isEmpty();
    }
}
