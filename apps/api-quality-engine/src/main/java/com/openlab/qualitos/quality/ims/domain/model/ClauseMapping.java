package com.openlab.qualitos.quality.ims.domain.model;

import java.util.Objects;

/**
 * Mapping bidirectionnel entre deux clauses normatives (CLAUDE.md §8.9).
 */
public record ClauseMapping(
        ClauseRef source,
        ClauseRef target,
        RelationType relationType,
        int confidence,
        String notes) {

    public ClauseMapping {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(relationType, "relationType");
        if (confidence < 0 || confidence > 100) {
            throw new IllegalArgumentException("confidence must be between 0 and 100");
        }
        if (source.equals(target)) {
            throw new IllegalArgumentException("source and target must differ (no self-loop)");
        }
    }
}
