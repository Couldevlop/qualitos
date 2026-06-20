package com.openlab.qualitos.quality.standards.auditblanc.domain;

import java.util.List;
import java.util.Objects;

/**
 * Commande de génération d'un audit blanc : la matière (norme adoptée + clauses
 * à risque avec leur état de preuve tenant) transmise au {@link MockAuditGenerator}
 * (Standards Hub §8.4 onglet 7). Value object PUR ; ne porte pas de tenant_id
 * (il vient du JWT côté passerelle IA).
 */
public record MockAuditGenerationCommand(
        String standardCode,
        String standardName,
        String industry,
        String language,
        int minQuestions,
        int maxQuestions,
        List<MockAuditClause> clauses) {

    public MockAuditGenerationCommand {
        if (standardCode == null || standardCode.isBlank()) {
            throw new IllegalArgumentException("standardCode required");
        }
        if (standardName == null || standardName.isBlank()) {
            throw new IllegalArgumentException("standardName required");
        }
        if (industry == null || industry.isBlank()) {
            throw new IllegalArgumentException("industry required");
        }
        if (clauses == null || clauses.isEmpty()) {
            throw new IllegalArgumentException("at least one clause required");
        }
        if (minQuestions < 1 || minQuestions > maxQuestions || maxQuestions > 200) {
            throw new IllegalArgumentException(
                    "require 1 <= minQuestions <= maxQuestions <= 200");
        }
        language = (language == null || language.isBlank()) ? "fr" : language;
        clauses = List.copyOf(clauses);
        Objects.requireNonNull(standardCode);
    }
}
