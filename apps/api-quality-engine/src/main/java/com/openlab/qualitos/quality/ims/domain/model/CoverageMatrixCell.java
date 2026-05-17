package com.openlab.qualitos.quality.ims.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Une cellule de la matrice de couverture IMS — pour une clause source donnée
 * vers une norme cible, la liste des clauses cible couvertes.
 */
public record CoverageMatrixCell(
        ClauseRef source,
        String targetStandardCode,
        List<TargetCoverage> coverages) {

    public CoverageMatrixCell {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(targetStandardCode, "targetStandardCode");
        coverages = coverages == null ? List.of() : List.copyOf(coverages);
    }

    public record TargetCoverage(String clauseCode, RelationType relation, int confidence) { }
}
