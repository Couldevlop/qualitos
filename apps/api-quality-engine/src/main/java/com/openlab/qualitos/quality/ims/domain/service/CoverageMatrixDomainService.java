package com.openlab.qualitos.quality.ims.domain.service;

import com.openlab.qualitos.quality.ims.domain.model.ClauseMapping;
import com.openlab.qualitos.quality.ims.domain.model.ClauseRef;
import com.openlab.qualitos.quality.ims.domain.model.CoverageMatrix;
import com.openlab.qualitos.quality.ims.domain.model.CoverageMatrixCell;
import com.openlab.qualitos.quality.ims.domain.model.RelationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service de domaine pur — agrège une liste de mappings en matrice de couverture.
 * Aucune dépendance Spring/JPA ; testable en isolation totale.
 */
public final class CoverageMatrixDomainService {

    /**
     * Construit la matrice de couverture pour un tenant donné, sur un set de normes.
     *
     * @param tenantId      identifiant tenant
     * @param standardCodes normes à inclure (filtre)
     * @param mappings      tous les mappings disponibles
     * @return matrice agrégée (cellule par clause source × norme cible)
     */
    public CoverageMatrix build(String tenantId, List<String> standardCodes, List<ClauseMapping> mappings) {
        Objects.requireNonNull(tenantId, "tenantId");
        List<String> safeCodes = standardCodes == null ? List.of() : List.copyOf(standardCodes);
        List<ClauseMapping> safeMappings = mappings == null ? List.of() : mappings;
        Set<String> codeSet = Set.copyOf(safeCodes);

        // Filtre : ne garder que les mappings dont la source ET la cible sont dans le set.
        List<ClauseMapping> filtered = safeMappings.stream()
                .filter(m -> codeSet.contains(m.source().standardCode())
                          && codeSet.contains(m.target().standardCode()))
                .toList();

        // Regroupe par (source, targetStandard) afin d'agréger les targetClauses.
        Map<GroupingKey, List<CoverageMatrixCell.TargetCoverage>> grouped = new LinkedHashMap<>();
        for (ClauseMapping m : filtered) {
            GroupingKey key = new GroupingKey(m.source(), m.target().standardCode());
            grouped.computeIfAbsent(key, k -> new ArrayList<>())
                   .add(new CoverageMatrixCell.TargetCoverage(
                           m.target().clauseCode(),
                           m.relationType(),
                           m.confidence()));
        }

        List<CoverageMatrixCell> cells = grouped.entrySet().stream()
                .map(e -> new CoverageMatrixCell(e.getKey().source(), e.getKey().targetStandard(), e.getValue()))
                .collect(Collectors.toList());

        int totalSources = (int) filtered.stream().map(ClauseMapping::source).distinct().count();
        return new CoverageMatrix(tenantId, safeCodes, cells, totalSources, filtered.size());
    }

    /**
     * Calcule le ratio de mutualisation (%): nombre de paires (source, target_norm)
     * où au moins une équivalence existe / produit cartésien sources × normes cible.
     * Utile pour mesurer l'effort économisé en approche IMS.
     */
    public double computeReuseRatio(CoverageMatrix matrix) {
        Objects.requireNonNull(matrix, "matrix");
        if (matrix.standardCodes().size() <= 1 || matrix.totalSourceClauses() == 0) {
            return 0.0;
        }
        Map<ClauseRef, Set<String>> coveredTargets = new HashMap<>();
        for (CoverageMatrixCell cell : matrix.cells()) {
            boolean hasEquivOrCovers = cell.coverages().stream()
                    .anyMatch(c -> c.relation() == RelationType.EQUIVALENT || c.relation() == RelationType.COVERS);
            if (hasEquivOrCovers) {
                coveredTargets.computeIfAbsent(cell.source(), k -> new java.util.HashSet<>())
                              .add(cell.targetStandardCode());
            }
        }
        int totalCovered = coveredTargets.values().stream().mapToInt(Set::size).sum();
        int maxPairs = matrix.totalSourceClauses() * (matrix.standardCodes().size() - 1);
        return maxPairs == 0 ? 0.0 : (double) totalCovered / (double) maxPairs * 100.0;
    }

    private record GroupingKey(ClauseRef source, String targetStandard) { }
}
