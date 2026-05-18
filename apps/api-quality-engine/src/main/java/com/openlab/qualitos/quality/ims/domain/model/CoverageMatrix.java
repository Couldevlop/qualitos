package com.openlab.qualitos.quality.ims.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Matrice de couverture IMS pour un tenant donné, restreinte à un set de
 * normes (toutes les normes adoptées par le tenant par défaut).
 *
 * Ne contient AUCUNE donnée sensible client — uniquement des codes de
 * normes/clauses publics. Néanmoins le calcul reste tenant-scopé (les normes
 * sélectionnées proviennent du contexte tenant via JWT).
 */
public record CoverageMatrix(
        String tenantId,
        List<String> standardCodes,
        List<CoverageMatrixCell> cells,
        int totalSourceClauses,
        int totalMappings) {

    public CoverageMatrix {
        Objects.requireNonNull(tenantId, "tenantId");
        standardCodes = standardCodes == null ? List.of() : List.copyOf(standardCodes);
        cells = cells == null ? List.of() : List.copyOf(cells);
    }
}
