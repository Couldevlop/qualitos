package com.openlab.qualitos.quality.standards.auditblanc.application;

import com.openlab.qualitos.quality.standards.auditblanc.domain.ClauseGapFinding;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditQuestion;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;
import com.openlab.qualitos.quality.standards.auditblanc.domain.RemediationAction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO de présentation de l'audit blanc IA avancé (Standards Hub §8.4 onglet 7).
 * Couche application : convertit l'agrégat {@link MockAuditRun} en vues immuables.
 */
public final class MockAuditDto {

    private MockAuditDto() {
    }

    public record QuestionView(String clauseCode, String question, String rationale) {
        static QuestionView of(MockAuditQuestion q) {
            return new QuestionView(q.clauseCode(), q.question(), q.rationale());
        }
    }

    public record GapView(
            String clauseCode,
            String title,
            String criticality,
            double coverageRatio,
            int totalRequirements,
            int coveredRequirements,
            String finding,
            List<QuestionView> questions) {
        static GapView of(ClauseGapFinding g) {
            return new GapView(
                    g.clauseCode(), g.title(), g.criticality().name(),
                    g.coverageRatio(), g.totalRequirements(), g.coveredRequirements(),
                    g.finding(), g.questions().stream().map(QuestionView::of).toList());
        }
    }

    public record RemediationActionView(
            String clauseCode,
            String criticality,
            String priority,
            String targetModule,
            String action) {
        static RemediationActionView of(RemediationAction a) {
            return new RemediationActionView(
                    a.clauseCode(), a.criticality().name(), a.priority(),
                    a.targetModule(), a.action());
        }
    }

    public record Report(
            UUID id,
            UUID adoptionId,
            UUID standardId,
            String standardCode,
            String standardName,
            double readiness,
            int majorCount,
            int minorCount,
            int observationCount,
            int questionCount,
            List<QuestionView> questions,
            List<GapView> gaps,
            List<RemediationActionView> remediationPlan,
            String aiProvider,
            UUID createdByUserId,
            Instant createdAt) {

        public static Report of(MockAuditRun run) {
            return new Report(
                    run.getId(), run.getAdoptionId(), run.getStandardId(),
                    run.getStandardCode(), run.getStandardName(), run.getReadiness(),
                    run.getMajorCount(), run.getMinorCount(), run.getObservationCount(),
                    run.getQuestionCount(),
                    run.getQuestions().stream().map(QuestionView::of).toList(),
                    run.getGaps().stream().map(GapView::of).toList(),
                    run.getRemediationPlan().stream().map(RemediationActionView::of).toList(),
                    run.getAiProvider(), run.getCreatedByUserId(), run.getCreatedAt());
        }
    }
}
