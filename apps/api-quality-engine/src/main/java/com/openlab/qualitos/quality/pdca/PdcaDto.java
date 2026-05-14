package com.openlab.qualitos.quality.pdca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PdcaDto {

    private PdcaDto() {}

    public record CreateCycleRequest(
            @NotBlank(message = "Title is required") String title,
            String description,
            @NotNull(message = "Owner ID is required") UUID ownerId
    ) {}

    public record StepRequest(
            @NotBlank(message = "Title is required") String title,
            String description,
            @NotNull(message = "Phase is required") PdcaPhase phase,
            StepStatus status,
            UUID assigneeId,
            LocalDate dueDate
    ) {}

    public record CycleResponse(
            UUID id,
            UUID tenantId,
            String title,
            String description,
            PdcaStatus status,
            UUID ownerId,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt,
            List<StepResponse> steps
    ) {}

    public record StepResponse(
            UUID id,
            UUID cycleId,
            PdcaPhase phase,
            String title,
            String description,
            StepStatus status,
            UUID assigneeId,
            LocalDate dueDate,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
