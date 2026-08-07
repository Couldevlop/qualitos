package com.openlab.qualitos.quality.fivewhys;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FiveWhysDto {

    record StepResponse(
            UUID id,
            int position,
            String answer,
            Instant createdAt,
            Instant updatedAt
    ) implements FiveWhysDto {}

    record AnalysisResponse(
            UUID id,
            UUID ncId,
            /** Référence lisible de la non-conformité : on l'affiche, pas l'UUID. */
            String ncReference,
            String problem,
            String rootCause,
            List<StepResponse> steps,
            Instant createdAt,
            Instant updatedAt
    ) implements FiveWhysDto {}

    record CreateRequest(
            @NotNull UUID ncId,
            /** Absent = on reprend le titre de la non-conformité. */
            @Size(max = 500) String problem
    ) implements FiveWhysDto {}

    record AddStepRequest(
            @NotBlank String answer
    ) implements FiveWhysDto {}

    record RootCauseRequest(
            @NotBlank String rootCause
    ) implements FiveWhysDto {}

    record UpdateProblemRequest(
            @NotBlank @Size(max = 500) String problem
    ) implements FiveWhysDto {}
}
