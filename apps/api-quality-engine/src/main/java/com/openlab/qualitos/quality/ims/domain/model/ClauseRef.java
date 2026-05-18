package com.openlab.qualitos.quality.ims.domain.model;

import java.util.Objects;

/**
 * Référence à une clause normative dans la couche domaine.
 * POJO immutable — aucune dépendance Spring/JPA.
 */
public record ClauseRef(String standardCode, String clauseCode) {
    public ClauseRef {
        Objects.requireNonNull(standardCode, "standardCode");
        Objects.requireNonNull(clauseCode, "clauseCode");
        if (standardCode.isBlank() || clauseCode.isBlank()) {
            throw new IllegalArgumentException("standardCode and clauseCode must be non-blank");
        }
    }
}
