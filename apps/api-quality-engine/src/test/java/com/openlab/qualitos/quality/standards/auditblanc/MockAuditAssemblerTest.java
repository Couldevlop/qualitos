package com.openlab.qualitos.quality.standards.auditblanc;

import com.openlab.qualitos.quality.standards.ObligationLevel;
import com.openlab.qualitos.quality.standards.RiskLevel;
import com.openlab.qualitos.quality.standards.auditblanc.domain.ClauseGapFinding;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditAssembler;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditClause;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditCriticality;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditQuestion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Gap analysis déterministe (§8.4 onglet 7). */
class MockAuditAssemblerTest {

    private static final MockAuditClause MAJOR = new MockAuditClause(
            "8.1", "Maîtrise opérationnelle", ObligationLevel.MUST, RiskLevel.CRITICAL, 4, 0, null);
    private static final MockAuditClause MINOR = new MockAuditClause(
            "7.1", "Ressources", ObligationLevel.MUST, RiskLevel.MEDIUM, 2, 1, null);
    private static final MockAuditClause OBS = new MockAuditClause(
            "4.1", "Contexte", ObligationLevel.SHOULD, RiskLevel.LOW, 2, 2, null);

    @Test
    void ordersByRisk_attachesQuestions_andUsesAiFinding() {
        List<MockAuditQuestion> questions = List.of(
                new MockAuditQuestion("8.1", "Comment maîtrisez-vous ?", "risque"));
        List<ClauseGapFinding> gaps = MockAuditAssembler.assemble(
                List.of(OBS, MINOR, MAJOR), questions, Map.of("8.1", "Constat IA 8.1."));

        assertThat(gaps.get(0).clauseCode()).isEqualTo("8.1");
        assertThat(gaps.get(0).finding()).isEqualTo("Constat IA 8.1.");
        assertThat(gaps.get(0).criticality()).isEqualTo(MockAuditCriticality.MAJOR);
        assertThat(gaps.get(0).questions()).singleElement()
                .extracting(MockAuditQuestion::question).isEqualTo("Comment maîtrisez-vous ?");
        assertThat(gaps.get(gaps.size() - 1).clauseCode()).isEqualTo("4.1");
        assertThat(gaps.get(gaps.size() - 1).criticality()).isEqualTo(MockAuditCriticality.OBSERVATION);
    }

    @Test
    void deterministicFallback_noAiFinding() {
        List<ClauseGapFinding> gaps = MockAuditAssembler.assemble(
                List.of(MAJOR, MINOR, OBS), List.of(), Map.of());
        Map<String, ClauseGapFinding> byCode = gaps.stream()
                .collect(java.util.stream.Collectors.toMap(ClauseGapFinding::clauseCode, g -> g));
        assertThat(byCode.get("8.1").finding()).contains("Aucune preuve");
        assertThat(byCode.get("7.1").finding()).contains("partielle");
        assertThat(byCode.get("4.1").finding()).contains("couverte");
    }

    @Test
    void blankAiFinding_fallsBackToDeterministic() {
        List<ClauseGapFinding> gaps = MockAuditAssembler.assemble(
                List.of(MAJOR), List.of(), Map.of("8.1", "  "));
        assertThat(gaps.get(0).finding()).contains("Aucune preuve");
    }

    @Test
    void countByCriticality() {
        List<ClauseGapFinding> gaps = MockAuditAssembler.assemble(
                List.of(MAJOR, MINOR, OBS), List.of(), Map.of());
        int[] counts = MockAuditAssembler.countByCriticality(gaps);
        assertThat(counts).containsExactly(1, 1, 1);
    }
}
