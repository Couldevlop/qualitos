package com.openlab.qualitos.quality.standards.auditblanc.domain;

/**
 * Question d'audit ciblée sur une clause à risque, rédigée par l'IA
 * (Standards Hub §8.4 onglet 7). Value object PUR.
 */
public record MockAuditQuestion(String clauseCode, String question, String rationale) {

    public MockAuditQuestion {
        if (clauseCode == null || clauseCode.isBlank()) {
            throw new IllegalArgumentException("clauseCode required");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question required");
        }
        rationale = rationale == null ? "" : rationale;
    }
}
