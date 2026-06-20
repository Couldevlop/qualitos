package com.openlab.qualitos.quality.standards.auditblanc;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.standards.ObligationLevel;
import com.openlab.qualitos.quality.standards.RiskLevel;
import com.openlab.qualitos.quality.standards.auditblanc.domain.GeneratedMockAudit;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditClause;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditGenerationCommand;
import com.openlab.qualitos.quality.standards.auditblanc.infrastructure.AiGatewayMockAuditGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Adapter de génération IA → passerelle (§8.4 onglet 7). */
@ExtendWith(MockitoExtension.class)
class AiGatewayMockAuditGeneratorTest {

    @Mock AiGatewayClient ai;
    @Captor ArgumentCaptor<Map<String, Object>> bodyCaptor;

    private MockAuditGenerationCommand command() {
        return new MockAuditGenerationCommand("iso-9001", "ISO 9001:2015", "manufacturing",
                "fr", 30, 100, List.of(
                new MockAuditClause("8.1", "Maîtrise", ObligationLevel.MUST, RiskLevel.CRITICAL,
                        4, 0, List.of("DOCUMENT")),
                new MockAuditClause("4.1", "Contexte", ObligationLevel.SHOULD, RiskLevel.LOW,
                        2, 2, null)));
    }

    @Test
    void buildsBody_andParsesResponse() {
        Map<String, Object> response = Map.of(
                "questions", List.of(
                        Map.of("clause_code", "8.1", "question", "Comment maîtrisez-vous ?",
                                "rationale", "risque"),
                        Map.of("clause_code", "8.1", "question", "")),     // vide → ignorée
                "gaps", List.of(
                        Map.of("clause_code", "8.1", "finding", "Aucune preuve."),
                        Map.of("clause_code", "4.1", "finding", "Couverte.")),
                "readiness", 25.0,
                "provider", "ollama");
        when(ai.mockAudit(bodyCaptor.capture(), eq(2))).thenReturn(response);

        AiGatewayMockAuditGenerator generator = new AiGatewayMockAuditGenerator(ai);
        GeneratedMockAudit out = generator.generate(command());

        // Corps : norme + clauses sérialisées en snake_case.
        Map<String, Object> body = bodyCaptor.getValue();
        assertThat(body).containsEntry("standard_code", "iso-9001")
                .containsEntry("industry", "manufacturing")
                .containsEntry("min_questions", 30);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> clauses = (List<Map<String, Object>>) body.get("clauses");
        assertThat(clauses).hasSize(2);
        assertThat(clauses.get(0)).containsEntry("clause_code", "8.1")
                .containsEntry("obligation", "must").containsEntry("risk", "critical")
                .containsEntry("total_requirements", 4).containsEntry("covered_requirements", 0);

        // Réponse : 1 question valide, constats projetés, readiness/provider.
        assertThat(out.questions()).singleElement()
                .satisfies(q -> assertThat(q.clauseCode()).isEqualTo("8.1"));
        assertThat(out.aiFindings()).containsEntry("8.1", "Aucune preuve.")
                .containsEntry("4.1", "Couverte.");
        assertThat(out.readiness()).isEqualTo(25.0);
        assertThat(out.provider()).isEqualTo("ollama");
    }

    @Test
    void tolerantToMissingOrMalformedFields() {
        // questions/gaps absents ou mal typés, readiness négatif → bornes sûres.
        when(ai.mockAudit(bodyCaptor.capture(), anyInt())).thenReturn(Map.of(
                "questions", "oops",
                "gaps", List.of("nope", Map.of("clause_code", "", "finding", "x"),
                        Map.of("clause_code", "8.1", "finding", "")),
                "readiness", -5,
                "provider", 42));
        GeneratedMockAudit out = new AiGatewayMockAuditGenerator(ai).generate(command());
        assertThat(out.questions()).isEmpty();
        assertThat(out.aiFindings()).isEmpty();
        assertThat(out.readiness()).isZero();           // négatif → 0
        assertThat(out.provider()).isEqualTo("42");
    }

    @Test
    void readinessClampedTo100() {
        when(ai.mockAudit(bodyCaptor.capture(), anyInt())).thenReturn(Map.of("readiness", 150.0));
        GeneratedMockAudit out = new AiGatewayMockAuditGenerator(ai).generate(command());
        assertThat(out.readiness()).isEqualTo(100d);
    }

    @Test
    void questionsAndGaps_notLists_yieldEmpty() {
        when(ai.mockAudit(bodyCaptor.capture(), anyInt())).thenReturn(Map.of(
                "questions", "nope", "gaps", "nope"));
        GeneratedMockAudit out = new AiGatewayMockAuditGenerator(ai).generate(command());
        assertThat(out.questions()).isEmpty();
        assertThat(out.aiFindings()).isEmpty();
    }

    @Test
    void question_blankClauseCode_isDropped() {
        when(ai.mockAudit(bodyCaptor.capture(), anyInt())).thenReturn(Map.of(
                "questions", List.of(
                        Map.of("clause_code", "", "question", "texte non vide"),  // code vide
                        "not-a-map"),
                "readiness", 10.0));
        GeneratedMockAudit out = new AiGatewayMockAuditGenerator(ai).generate(command());
        assertThat(out.questions()).isEmpty();
        assertThat(out.readiness()).isEqualTo(10d);
    }

    @Test
    void readinessNonNumber_isZero() {
        when(ai.mockAudit(bodyCaptor.capture(), anyInt())).thenReturn(Map.of(
                "readiness", "vingt-cinq"));
        GeneratedMockAudit out = new AiGatewayMockAuditGenerator(ai).generate(command());
        assertThat(out.readiness()).isZero();
    }
}
