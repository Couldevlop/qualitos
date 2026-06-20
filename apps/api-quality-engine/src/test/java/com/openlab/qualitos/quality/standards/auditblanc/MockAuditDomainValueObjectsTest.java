package com.openlab.qualitos.quality.standards.auditblanc;

import com.openlab.qualitos.quality.standards.ObligationLevel;
import com.openlab.qualitos.quality.standards.RiskLevel;
import com.openlab.qualitos.quality.standards.auditblanc.domain.ClauseGapFinding;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditClause;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditCriticality;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditGenerationCommand;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditQuestion;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;
import com.openlab.qualitos.quality.standards.auditblanc.domain.RemediationAction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Value objects + agrégat du domaine audit blanc (§8.4 onglet 7). */
class MockAuditDomainValueObjectsTest {

    // ---- MockAuditQuestion ----

    @Test
    void question_validation() {
        assertThatThrownBy(() -> new MockAuditQuestion(" ", "q", null))
                .hasMessageContaining("clauseCode");
        assertThatThrownBy(() -> new MockAuditQuestion(null, "q", null))
                .hasMessageContaining("clauseCode");
        assertThatThrownBy(() -> new MockAuditQuestion("4.1", " ", null))
                .hasMessageContaining("question");
        assertThatThrownBy(() -> new MockAuditQuestion("4.1", null, null))
                .hasMessageContaining("question");
        assertThat(new MockAuditQuestion("4.1", "q", null).rationale()).isEmpty();
        assertThat(new MockAuditQuestion("4.1", "q", "raison").rationale()).isEqualTo("raison");
    }

    // ---- ClauseGapFinding ----

    @Test
    void gap_validation() {
        assertThatThrownBy(() -> new ClauseGapFinding(" ", "t",
                MockAuditCriticality.MINOR, 0.5, 2, 1, "f", null))
                .hasMessageContaining("clauseCode");
        assertThatThrownBy(() -> new ClauseGapFinding(null, "t",
                MockAuditCriticality.MINOR, 0.5, 2, 1, "f", null))
                .hasMessageContaining("clauseCode");
        assertThatThrownBy(() -> new ClauseGapFinding("4.1", "t",
                MockAuditCriticality.MINOR, 0.5, 2, 1, " ", null))
                .hasMessageContaining("finding");
        assertThatThrownBy(() -> new ClauseGapFinding("4.1", "t",
                MockAuditCriticality.MINOR, 0.5, 2, 1, null, null))
                .hasMessageContaining("finding");
        assertThatThrownBy(() -> new ClauseGapFinding("4.1", "t",
                MockAuditCriticality.MINOR, 1.5, 2, 1, "f", null))
                .hasMessageContaining("coverageRatio");
        assertThatThrownBy(() -> new ClauseGapFinding("4.1", "t",
                MockAuditCriticality.MINOR, -0.1, 2, 1, "f", null))
                .hasMessageContaining("coverageRatio");
        ClauseGapFinding g = new ClauseGapFinding("4.1", "t",
                MockAuditCriticality.MINOR, 0.5, 2, 1, "f", null);
        assertThat(g.questions()).isEmpty();
        ClauseGapFinding withQ = new ClauseGapFinding("4.1", "t",
                MockAuditCriticality.MINOR, 0.5, 2, 1, "f",
                List.of(new MockAuditQuestion("4.1", "q", null)));
        assertThat(withQ.questions()).hasSize(1);
    }

    // ---- RemediationAction ----

    @Test
    void remediationAction_validation() {
        assertThatThrownBy(() -> new RemediationAction(" ", MockAuditCriticality.MAJOR,
                "high", "AUDIT", "a")).hasMessageContaining("clauseCode");
        assertThatThrownBy(() -> new RemediationAction(null, MockAuditCriticality.MAJOR,
                "high", "AUDIT", "a")).hasMessageContaining("clauseCode");
        assertThatThrownBy(() -> new RemediationAction("8.1", MockAuditCriticality.MAJOR,
                "high", "AUDIT", " ")).hasMessageContaining("action");
        assertThatThrownBy(() -> new RemediationAction("8.1", MockAuditCriticality.MAJOR,
                "high", "AUDIT", null)).hasMessageContaining("action");
        assertThatThrownBy(() -> new RemediationAction("8.1", MockAuditCriticality.MAJOR,
                " ", "AUDIT", "a")).hasMessageContaining("priority");
        assertThatThrownBy(() -> new RemediationAction("8.1", MockAuditCriticality.MAJOR,
                null, "AUDIT", "a")).hasMessageContaining("priority");
        assertThatThrownBy(() -> new RemediationAction("8.1", MockAuditCriticality.MAJOR,
                "high", " ", "a")).hasMessageContaining("targetModule");
        assertThatThrownBy(() -> new RemediationAction("8.1", MockAuditCriticality.MAJOR,
                "high", null, "a")).hasMessageContaining("targetModule");
        assertThat(new RemediationAction("8.1", MockAuditCriticality.MAJOR,
                "high", "AUDIT", "a").action()).isEqualTo("a");
    }

    // ---- MockAuditCriticality ----

    @Test
    void criticality_ranksAndPriorities() {
        assertThat(MockAuditCriticality.MAJOR.rank()).isZero();
        assertThat(MockAuditCriticality.MINOR.rank()).isEqualTo(1);
        assertThat(MockAuditCriticality.OBSERVATION.rank()).isEqualTo(2);
        assertThat(MockAuditCriticality.MAJOR.remediationPriority()).isEqualTo("high");
        assertThat(MockAuditCriticality.MINOR.remediationPriority()).isEqualTo("medium");
        assertThat(MockAuditCriticality.OBSERVATION.remediationPriority()).isEqualTo("low");
    }

    // ---- MockAuditGenerationCommand ----

    private static MockAuditClause clause() {
        return new MockAuditClause("8.1", "Maîtrise", ObligationLevel.MUST,
                RiskLevel.HIGH, 2, 0, null);
    }

    @Test
    void command_validation() {
        assertThatThrownBy(() -> new MockAuditGenerationCommand(" ", "n", "i", "fr", 30, 100, List.of(clause())))
                .hasMessageContaining("standardCode");
        assertThatThrownBy(() -> new MockAuditGenerationCommand(null, "n", "i", "fr", 30, 100, List.of(clause())))
                .hasMessageContaining("standardCode");
        assertThatThrownBy(() -> new MockAuditGenerationCommand("c", " ", "i", "fr", 30, 100, List.of(clause())))
                .hasMessageContaining("standardName");
        assertThatThrownBy(() -> new MockAuditGenerationCommand("c", null, "i", "fr", 30, 100, List.of(clause())))
                .hasMessageContaining("standardName");
        assertThatThrownBy(() -> new MockAuditGenerationCommand("c", "n", " ", "fr", 30, 100, List.of(clause())))
                .hasMessageContaining("industry");
        assertThatThrownBy(() -> new MockAuditGenerationCommand("c", "n", null, "fr", 30, 100, List.of(clause())))
                .hasMessageContaining("industry");
        assertThatThrownBy(() -> new MockAuditGenerationCommand("c", "n", "i", "fr", 30, 100, List.of()))
                .hasMessageContaining("at least one clause");
        assertThatThrownBy(() -> new MockAuditGenerationCommand("c", "n", "i", "fr", 30, 100, null))
                .hasMessageContaining("at least one clause");
        // Bornes de questions : min<1, min>max, max>200.
        assertThatThrownBy(() -> new MockAuditGenerationCommand("c", "n", "i", "fr", 0, 100, List.of(clause())))
                .hasMessageContaining("minQuestions");
        assertThatThrownBy(() -> new MockAuditGenerationCommand("c", "n", "i", "fr", 50, 40, List.of(clause())))
                .hasMessageContaining("minQuestions");
        assertThatThrownBy(() -> new MockAuditGenerationCommand("c", "n", "i", "fr", 1, 300, List.of(clause())))
                .hasMessageContaining("minQuestions");
        // language null/blank → défaut "fr" ; valeur explicite préservée.
        assertThat(new MockAuditGenerationCommand("c", "n", "i", null, 30, 100, List.of(clause())).language())
                .isEqualTo("fr");
        assertThat(new MockAuditGenerationCommand("c", "n", "i", " ", 30, 100, List.of(clause())).language())
                .isEqualTo("fr");
        assertThat(new MockAuditGenerationCommand("c", "n", "i", "en", 30, 100, List.of(clause())).language())
                .isEqualTo("en");
    }

    // ---- MockAuditRun ----

    @Test
    void run_factory_countsCriticalitiesAndExposesFields() {
        UUID tenant = UUID.randomUUID();
        UUID adoption = UUID.randomUUID();
        UUID standard = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-20T10:00:00Z");

        List<ClauseGapFinding> gaps = List.of(
                new ClauseGapFinding("8.1", "Maîtrise", MockAuditCriticality.MAJOR, 0d, 4, 0, "f", null),
                new ClauseGapFinding("7.1", "Ressources", MockAuditCriticality.MINOR, 0.5, 2, 1, "f", null),
                new ClauseGapFinding("4.1", "Contexte", MockAuditCriticality.OBSERVATION, 1d, 2, 2, "f", null));
        List<MockAuditQuestion> questions = List.of(new MockAuditQuestion("8.1", "q", null));
        List<RemediationAction> plan = List.of(
                new RemediationAction("8.1", MockAuditCriticality.MAJOR, "high", "AUDIT", "a"));

        MockAuditRun run = MockAuditRun.of(tenant, adoption, standard, "iso-9001",
                "ISO 9001:2015", 42.5d, questions, gaps, plan, "ollama", actor, now);

        assertThat(run.getId()).isNull();
        assertThat(run.getMajorCount()).isEqualTo(1);
        assertThat(run.getMinorCount()).isEqualTo(1);
        assertThat(run.getObservationCount()).isEqualTo(1);
        assertThat(run.getQuestionCount()).isEqualTo(1);
        assertThat(run.getReadiness()).isEqualTo(42.5d);
        assertThat(run.getStandardCode()).isEqualTo("iso-9001");
        assertThat(run.getStandardName()).isEqualTo("ISO 9001:2015");
        assertThat(run.getAiProvider()).isEqualTo("ollama");
        assertThat(run.getCreatedByUserId()).isEqualTo(actor);
        assertThat(run.getCreatedAt()).isEqualTo(now);
        assertThat(run.getTenantId()).isEqualTo(tenant);
        assertThat(run.getAdoptionId()).isEqualTo(adoption);
        assertThat(run.getStandardId()).isEqualTo(standard);

        run.assignId(UUID.randomUUID());
        assertThat(run.getId()).isNotNull();
    }

    @Test
    void run_readinessOutOfRange_rejected() {
        assertThatThrownBy(() -> new MockAuditRun(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "c", "n", 120d, 0, 0, 0,
                List.of(), List.of(), List.of(), "ollama", UUID.randomUUID(), Instant.now()))
                .hasMessageContaining("readiness");
    }

    @Test
    void run_blankCode_andNegativeCount_rejected() {
        assertThatThrownBy(() -> new MockAuditRun(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), " ", "n", 10d, 0, 0, 0,
                null, null, null, "ollama", UUID.randomUUID(), Instant.now()))
                .hasMessageContaining("standardCode");
        assertThatThrownBy(() -> new MockAuditRun(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "c", " ", 10d, 0, 0, 0,
                List.of(), List.of(), List.of(), "ollama", UUID.randomUUID(), Instant.now()))
                .hasMessageContaining("standardName");
        assertThatThrownBy(() -> new MockAuditRun(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), null, "n", 10d, 0, 0, 0,
                List.of(), List.of(), List.of(), "ollama", UUID.randomUUID(), Instant.now()))
                .hasMessageContaining("standardCode");
        assertThatThrownBy(() -> new MockAuditRun(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "c", null, 10d, 0, 0, 0,
                List.of(), List.of(), List.of(), "ollama", UUID.randomUUID(), Instant.now()))
                .hasMessageContaining("standardName");
        assertThatThrownBy(() -> new MockAuditRun(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "c", "n", -1d, 0, 0, 0,
                List.of(), List.of(), List.of(), "ollama", UUID.randomUUID(), Instant.now()))
                .hasMessageContaining("readiness");
        assertThatThrownBy(() -> new MockAuditRun(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "c", "n", 10d, -1, 0, 0,
                List.of(), List.of(), List.of(), "ollama", UUID.randomUUID(), Instant.now()))
                .hasMessageContaining("majorCount");
        assertThatThrownBy(() -> new MockAuditRun(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "c", "n", 10d, 0, -1, 0,
                List.of(), List.of(), List.of(), "ollama", UUID.randomUUID(), Instant.now()))
                .hasMessageContaining("minorCount");
        assertThatThrownBy(() -> new MockAuditRun(null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "c", "n", 10d, 0, 0, -1,
                List.of(), List.of(), List.of(), "ollama", UUID.randomUUID(), Instant.now()))
                .hasMessageContaining("observationCount");
    }

    @Test
    void run_aiProvider_nonNull_preserved() {
        MockAuditRun run = new MockAuditRun(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "c", "n", 10d, 0, 0, 0,
                List.of(), List.of(), List.of(), "anthropic", UUID.randomUUID(), Instant.now());
        assertThat(run.getAiProvider()).isEqualTo("anthropic");
    }

    @Test
    void run_nullCollections_defaultEmpty() {
        MockAuditRun run = new MockAuditRun(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "c", "n", 10d, 0, 0, 0,
                null, null, null, null, UUID.randomUUID(), Instant.now());
        assertThat(run.getQuestions()).isEmpty();
        assertThat(run.getGaps()).isEmpty();
        assertThat(run.getRemediationPlan()).isEmpty();
        assertThat(run.getAiProvider()).isEmpty();
    }
}
