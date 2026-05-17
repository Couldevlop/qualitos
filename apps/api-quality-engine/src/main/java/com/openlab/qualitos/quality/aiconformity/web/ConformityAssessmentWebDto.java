package com.openlab.qualitos.quality.aiconformity.web;

import com.openlab.qualitos.quality.aiconformity.domain.ConformityProcedure;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ConformityAssessmentWebDto {

    private ConformityAssessmentWebDto() {}

    public record PlanRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotNull UUID aiSystemId,
            UUID qmsId,
            @NotNull ConformityProcedure procedure,
            @Size(max = 8) @Pattern(regexp = "^$|^[0-9]{4}$",
                    message = "notifiedBodyId must be 4 digits")
            String notifiedBodyId,
            @Size(max = 250) String notifiedBodyName,
            @NotBlank @Size(max = 4000) String scope,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            UUID qmsId,
            @Size(max = 8) @Pattern(regexp = "^$|^[0-9]{4}$",
                    message = "notifiedBodyId must be 4 digits")
            String notifiedBodyId,
            @Size(max = 250) String notifiedBodyName,
            @NotBlank @Size(max = 4000) String scope) {}

    public record CertifyRequest(
            @NotBlank @Size(max = 250) String certificateNumber,
            @NotBlank @Size(max = 250) String euDeclarationReference,
            @NotNull @Future Instant validUntil) {}

    public record RevokeRequest(@NotBlank @Size(max = 2000) String reason) {}

    public record FailRequest(@NotBlank @Size(max = 2000) String reason) {}
}
