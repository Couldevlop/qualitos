package com.openlab.qualitos.quality.aieudb.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class EudbRegistrationWebDto {

    private EudbRegistrationWebDto() {}

    public record DraftRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotNull UUID aiSystemId,
            @Size(max = 250) String providerEntityName,
            @Size(max = 250) String providerEuRepresentative,
            @Size(max = 2) @Pattern(regexp = "^$|^[A-Z]{2}$",
                    message = "memberStateOfReference must be ISO-3166-1 alpha-2")
            String memberStateOfReference,
            @Size(max = 4000) String intendedPurposeSummary,
            @Size(max = 250) String technicalDocumentationReference,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @Size(max = 250) String providerEntityName,
            @Size(max = 250) String providerEuRepresentative,
            @Size(max = 2) @Pattern(regexp = "^$|^[A-Z]{2}$",
                    message = "memberStateOfReference must be ISO-3166-1 alpha-2")
            String memberStateOfReference,
            @Size(max = 4000) String intendedPurposeSummary,
            @Size(max = 250) String technicalDocumentationReference) {}

    public record SubmitRequest(@NotNull UUID submittedByUserId) {}

    public record MarkRegisteredRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^EUDB-AI-[A-Z0-9]{6,32}$",
                    message = "eudbId must match EUDB-AI-[A-Z0-9]{6,32}")
            String eudbId,
            @NotNull Instant registrationDate) {}

    public record DeclareUpdateRequest(
            @NotBlank @Size(max = 4000) String updateSummary,
            @NotNull Instant updateDate) {}

    public record RejectRequest(@NotBlank @Size(max = 2000) String reason) {}

    public record RetireRequest(@NotBlank @Size(max = 2000) String reason) {}
}
