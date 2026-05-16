package com.openlab.qualitos.quality.aiactfria.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class FriaWebDto {

    private FriaWebDto() {}

    public record DraftRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotNull UUID aiSystemId,
            @NotBlank @Size(max = 4000) String processDescription,
            @Size(max = 4000) String deploymentDurationDescription,
            @NotBlank @Size(max = 4000) String affectedPersonsCategories,
            @NotBlank @Size(max = 4000) String specificRisks,
            @Size(max = 4000) String mitigationMeasures,
            @Size(max = 4000) String humanOversightMeasures,
            @Size(max = 4000) String complaintMechanismDescription,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 4000) String processDescription,
            @Size(max = 4000) String deploymentDurationDescription,
            @NotBlank @Size(max = 4000) String affectedPersonsCategories,
            @NotBlank @Size(max = 4000) String specificRisks,
            @Size(max = 4000) String mitigationMeasures,
            @Size(max = 4000) String humanOversightMeasures,
            @Size(max = 4000) String complaintMechanismDescription) {}

    public record SubmitRequest(@NotNull UUID submittedByUserId) {}

    public record ApproveRequest(
            @NotNull UUID approvedByUserId,
            @Size(max = 4000) String approvalNotes) {}

    public record ReturnRequest(@NotBlank @Size(max = 4000) String reason) {}

    public record ArchiveRequest(@NotBlank @Size(max = 2000) String reason) {}
}
