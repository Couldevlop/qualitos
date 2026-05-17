package com.openlab.qualitos.quality.ims.presentation.dto;

import com.openlab.qualitos.quality.ims.domain.model.CoverageMatrix;
import com.openlab.qualitos.quality.ims.domain.model.CoverageMatrixCell;
import com.openlab.qualitos.quality.ims.domain.model.RelationType;

import java.util.List;

public final class CoverageMatrixDto {

    private CoverageMatrixDto() {}

    public record CoverageMatrixResponse(
            String tenantId,
            List<String> standardCodes,
            List<CellDto> cells,
            int totalSourceClauses,
            int totalMappings,
            double reuseRatioPercent) { }

    public record CellDto(
            String sourceStandardCode,
            String sourceClauseCode,
            String targetStandardCode,
            List<TargetDto> targets) { }

    public record TargetDto(String clauseCode, RelationType relation, int confidence) { }

    public static CoverageMatrixResponse from(CoverageMatrix matrix, double reuseRatio) {
        List<CellDto> cells = matrix.cells().stream()
                .map(CoverageMatrixDto::toCell)
                .toList();
        return new CoverageMatrixResponse(
                matrix.tenantId(),
                matrix.standardCodes(),
                cells,
                matrix.totalSourceClauses(),
                matrix.totalMappings(),
                reuseRatio
        );
    }

    private static CellDto toCell(CoverageMatrixCell c) {
        List<TargetDto> targets = c.coverages().stream()
                .map(t -> new TargetDto(t.clauseCode(), t.relation(), t.confidence()))
                .toList();
        return new CellDto(c.source().standardCode(), c.source().clauseCode(),
                c.targetStandardCode(), targets);
    }
}
