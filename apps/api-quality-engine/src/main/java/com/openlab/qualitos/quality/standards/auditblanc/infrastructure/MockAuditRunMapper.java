package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.auditblanc.domain.ClauseGapFinding;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditCriticality;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditQuestion;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;
import com.openlab.qualitos.quality.standards.auditblanc.domain.RemediationAction;

import java.util.List;

/**
 * Conversion entité JPA ⇄ agrégat de domaine. Questions, écarts et plan de
 * remédiation sont (dé)sérialisés en JSON via Jackson. Mapper stateless ;
 * l'{@link ObjectMapper} est injecté.
 */
final class MockAuditRunMapper {

    private static final TypeReference<List<QuestionDto>> QUESTION_LIST = new TypeReference<>() { };
    private static final TypeReference<List<GapDto>> GAP_LIST = new TypeReference<>() { };
    private static final TypeReference<List<ActionDto>> ACTION_LIST = new TypeReference<>() { };

    private MockAuditRunMapper() {
    }

    record QuestionDto(String clauseCode, String question, String rationale) {
        static QuestionDto of(MockAuditQuestion q) {
            return new QuestionDto(q.clauseCode(), q.question(), q.rationale());
        }

        MockAuditQuestion toDomain() {
            return new MockAuditQuestion(clauseCode, question, rationale);
        }
    }

    record GapDto(String clauseCode, String title, MockAuditCriticality criticality,
                  double coverageRatio, int totalRequirements, int coveredRequirements,
                  String finding, List<QuestionDto> questions) {
        static GapDto of(ClauseGapFinding g) {
            return new GapDto(g.clauseCode(), g.title(), g.criticality(),
                    g.coverageRatio(), g.totalRequirements(), g.coveredRequirements(),
                    g.finding(), g.questions().stream().map(QuestionDto::of).toList());
        }

        ClauseGapFinding toDomain() {
            return new ClauseGapFinding(clauseCode, title, criticality, coverageRatio,
                    totalRequirements, coveredRequirements, finding,
                    questions.stream().map(QuestionDto::toDomain).toList());
        }
    }

    record ActionDto(String clauseCode, MockAuditCriticality criticality, String priority,
                     String targetModule, String action) {
        static ActionDto of(RemediationAction a) {
            return new ActionDto(a.clauseCode(), a.criticality(), a.priority(),
                    a.targetModule(), a.action());
        }

        RemediationAction toDomain() {
            return new RemediationAction(clauseCode, criticality, priority, targetModule, action);
        }
    }

    static MockAuditRunJpaEntity toEntity(MockAuditRun r, ObjectMapper json) {
        MockAuditRunJpaEntity e = new MockAuditRunJpaEntity();
        if (r.getId() != null) {
            e.setId(r.getId());
        }
        e.setTenantId(r.getTenantId());
        e.setAdoptionId(r.getAdoptionId());
        e.setStandardId(r.getStandardId());
        e.setStandardCode(r.getStandardCode());
        e.setStandardName(r.getStandardName());
        e.setReadiness(r.getReadiness());
        e.setMajorCount(r.getMajorCount());
        e.setMinorCount(r.getMinorCount());
        e.setObservationCount(r.getObservationCount());
        e.setQuestionCount(r.getQuestionCount());
        e.setQuestionsJson(write(r.getQuestions().stream().map(QuestionDto::of).toList(), json));
        e.setGapsJson(write(r.getGaps().stream().map(GapDto::of).toList(), json));
        e.setRemediationJson(write(r.getRemediationPlan().stream().map(ActionDto::of).toList(), json));
        e.setAiProvider(r.getAiProvider());
        e.setCreatedByUserId(r.getCreatedByUserId());
        e.setCreatedAt(r.getCreatedAt());
        return e;
    }

    static MockAuditRun toDomain(MockAuditRunJpaEntity e, ObjectMapper json) {
        List<MockAuditQuestion> questions = read(e.getQuestionsJson(), QUESTION_LIST, json)
                .stream().map(QuestionDto::toDomain).toList();
        List<ClauseGapFinding> gaps = read(e.getGapsJson(), GAP_LIST, json)
                .stream().map(GapDto::toDomain).toList();
        List<RemediationAction> plan = read(e.getRemediationJson(), ACTION_LIST, json)
                .stream().map(ActionDto::toDomain).toList();
        return new MockAuditRun(
                e.getId(), e.getTenantId(), e.getAdoptionId(), e.getStandardId(),
                e.getStandardCode(), e.getStandardName(), e.getReadiness(),
                e.getMajorCount(), e.getMinorCount(), e.getObservationCount(),
                questions, gaps, plan, e.getAiProvider(),
                e.getCreatedByUserId(), e.getCreatedAt());
    }

    private static String write(Object value, ObjectMapper json) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize mock audit payload", ex);
        }
    }

    private static <T> List<T> read(String value, TypeReference<List<T>> type, ObjectMapper json) {
        try {
            return json.readValue(value, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot deserialize mock audit payload", ex);
        }
    }
}
