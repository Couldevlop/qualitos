package com.openlab.qualitos.quality.ishikawa;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IshikawaDto {

    private IshikawaDto() {}

    public record CreateDiagramRequest(
            @NotBlank(message = "Problem statement is required")
            @Size(max = 500, message = "Problem statement cannot exceed 500 characters")
            String problemStatement,
            String description,
            IshikawaMode mode,
            @NotNull(message = "Owner ID is required") UUID ownerId
    ) {}

    public record UpdateDiagramRequest(
            @Size(max = 500, message = "Problem statement cannot exceed 500 characters")
            String problemStatement,
            String description,
            IshikawaMode mode,
            IshikawaStatus status
    ) {}

    public record CauseRequest(
            @NotNull(message = "Category is required") CauseCategory category,
            @NotBlank(message = "Label is required")
            @Size(max = 500, message = "Label cannot exceed 500 characters") String label,
            String description,
            UUID parentId,
            @DecimalMin(value = "0.0", message = "rootCauseScore must be >= 0")
            @DecimalMax(value = "1.0", message = "rootCauseScore must be <= 1")
            Double rootCauseScore
    ) {}

    public record UpdateCauseRequest(
            CauseCategory category,
            @Size(max = 500, message = "Label cannot exceed 500 characters") String label,
            String description,
            @DecimalMin(value = "0.0", message = "rootCauseScore must be >= 0")
            @DecimalMax(value = "1.0", message = "rootCauseScore must be <= 1")
            Double rootCauseScore
    ) {}

    public record DiagramResponse(
            UUID id,
            UUID tenantId,
            String problemStatement,
            String description,
            IshikawaMode mode,
            IshikawaStatus status,
            UUID ownerId,
            Instant createdAt,
            Instant updatedAt,
            List<CauseResponse> causes
    ) {}

    public record CauseResponse(
            UUID id,
            UUID diagramId,
            UUID parentId,
            CauseCategory category,
            String label,
            String description,
            Double rootCauseScore,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
