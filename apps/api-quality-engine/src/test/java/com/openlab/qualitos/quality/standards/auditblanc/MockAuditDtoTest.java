package com.openlab.qualitos.quality.standards.auditblanc;

import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditDto;
import com.openlab.qualitos.quality.standards.auditblanc.domain.ClauseGapFinding;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditCriticality;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditQuestion;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;
import com.openlab.qualitos.quality.standards.auditblanc.domain.RemediationAction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Conversion agrégat → vues de présentation (§8.4 onglet 7). */
class MockAuditDtoTest {

    @Test
    void report_mapsAllNestedViews() {
        UUID id = UUID.randomUUID();
        List<MockAuditQuestion> questions = List.of(
                new MockAuditQuestion("8.1", "Q ?", "raison"));
        List<ClauseGapFinding> gaps = List.of(
                new ClauseGapFinding("8.1", "Maîtrise", MockAuditCriticality.MAJOR, 0.0, 4, 0,
                        "Aucune preuve.", questions));
        List<RemediationAction> plan = List.of(
                new RemediationAction("8.1", MockAuditCriticality.MAJOR, "high", "AUDIT", "Lever NC."));
        MockAuditRun run = MockAuditRun.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "iso-9001", "ISO 9001:2015", 20d, questions, gaps, plan, "ollama",
                UUID.randomUUID(), Instant.parse("2026-06-20T09:00:00Z"));
        run.assignId(id);

        MockAuditDto.Report report = MockAuditDto.Report.of(run);

        assertThat(report.id()).isEqualTo(id);
        assertThat(report.standardCode()).isEqualTo("iso-9001");
        assertThat(report.questionCount()).isEqualTo(1);
        assertThat(report.questions()).singleElement().satisfies(q -> {
            assertThat(q.clauseCode()).isEqualTo("8.1");
            assertThat(q.rationale()).isEqualTo("raison");
        });
        assertThat(report.gaps()).singleElement().satisfies(g -> {
            assertThat(g.criticality()).isEqualTo("MAJOR");
            assertThat(g.coverageRatio()).isZero();
            assertThat(g.questions()).hasSize(1);
        });
        assertThat(report.remediationPlan()).singleElement().satisfies(a -> {
            assertThat(a.criticality()).isEqualTo("MAJOR");
            assertThat(a.targetModule()).isEqualTo("AUDIT");
            assertThat(a.priority()).isEqualTo("high");
        });
    }
}
