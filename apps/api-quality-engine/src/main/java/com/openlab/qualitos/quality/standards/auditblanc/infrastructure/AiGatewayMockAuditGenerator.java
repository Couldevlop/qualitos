package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.standards.auditblanc.domain.GeneratedMockAudit;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditClause;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditGenerationCommand;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditGenerator;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditQuestion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter du port {@link MockAuditGenerator} : relaie la matière d'audit à la
 * passerelle IA réelle ({@link AiGatewayClient} → {@code ai-service}, endpoint
 * {@code /v1/ai/standards/mock-audit}). L'IA génère réellement les questions et
 * les constats (§8.4 onglet 7) ; aucune question/écart en dur. La passerelle
 * applique la redaction PII + le bouclier anti-injection ; le tenant provient du
 * JWT (jamais du body, §18.2 #2/#4).
 *
 * <p>L'ai-service ne renvoie QUE des clauses connues (anti-hallucination). Ici on
 * mappe défensivement la réponse JSON : codes inconnus ou champs vides ignorés.
 */
@Component
public class AiGatewayMockAuditGenerator implements MockAuditGenerator {

    private final AiGatewayClient ai;

    public AiGatewayMockAuditGenerator(AiGatewayClient ai) {
        this.ai = ai;
    }

    @Override
    public GeneratedMockAudit generate(MockAuditGenerationCommand command) {
        Map<String, Object> body = buildBody(command);
        Map<String, Object> resp = ai.mockAudit(body, command.clauses().size());

        List<MockAuditQuestion> questions = parseQuestions(resp);
        Map<String, String> aiFindings = parseFindings(resp);
        double readiness = asDouble(resp.get("readiness"));
        String provider = asString(resp.get("provider"));

        return new GeneratedMockAudit(questions, aiFindings, readiness, provider);
    }

    private static Map<String, Object> buildBody(MockAuditGenerationCommand c) {
        List<Map<String, Object>> clauses = new ArrayList<>(c.clauses().size());
        for (MockAuditClause cl : c.clauses()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clause_code", cl.clauseCode());
            m.put("title", cl.title());
            m.put("obligation", cl.obligation().name().toLowerCase(Locale.ROOT));
            m.put("risk", cl.risk().name().toLowerCase(Locale.ROOT));
            m.put("total_requirements", cl.totalRequirements());
            m.put("covered_requirements", cl.coveredRequirements());
            m.put("evidence_types", cl.evidenceTypes());
            clauses.add(m);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("standard_code", c.standardCode());
        body.put("standard_name", c.standardName());
        body.put("industry", c.industry());
        body.put("language", c.language());
        body.put("min_questions", c.minQuestions());
        body.put("max_questions", c.maxQuestions());
        body.put("clauses", clauses);
        return body;
    }

    @SuppressWarnings("unchecked")
    private static List<MockAuditQuestion> parseQuestions(Map<String, Object> resp) {
        Object raw = resp.get("questions");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<MockAuditQuestion> questions = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) {
                continue;
            }
            String code = asString(((Map<String, Object>) m).get("clause_code"));
            String text = asString(((Map<String, Object>) m).get("question"));
            if (code.isBlank() || text.isBlank()) {
                continue;
            }
            questions.add(new MockAuditQuestion(code, text,
                    asString(((Map<String, Object>) m).get("rationale"))));
        }
        return questions;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> parseFindings(Map<String, Object> resp) {
        Map<String, String> findings = new LinkedHashMap<>();
        // L'ai-service renvoie les constats dans gaps[].finding (par clause).
        Object raw = resp.get("gaps");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) {
                    continue;
                }
                String code = asString(((Map<String, Object>) m).get("clause_code"));
                String finding = asString(((Map<String, Object>) m).get("finding"));
                if (!code.isBlank() && !finding.isBlank()) {
                    findings.put(code, finding);
                }
            }
        }
        return findings;
    }

    private static String asString(Object o) {
        return o == null ? "" : String.valueOf(o).strip();
    }

    private static double asDouble(Object o) {
        if (o instanceof Number n) {
            double v = n.doubleValue();
            if (v < 0d) {
                return 0d;
            }
            return Math.min(v, 100d);
        }
        return 0d;
    }
}
