package com.openlab.qualitos.quality.processoragreements.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class ProcessorAgreementWebDto {

    private ProcessorAgreementWebDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotBlank @Size(max = 250) String processorName,
            @Size(max = 250) String processorLegalEntity,
            @Size(max = 250) String processorContact,
            @Size(max = 250) String processorDpoContact,
            @Size(min = 2, max = 2)
            @Pattern(regexp = "^[A-Za-z]{2}$") String processorCountry,
            @NotBlank @Size(max = 4000) String servicesDescription,
            Set<String> subProcessorCategories,
            Set<UUID> linkedProcessingActivityIds,
            Set<String> thirdCountryTransfers,
            @Size(max = 4000) String transferSafeguards,
            @Size(max = 1024) String contractDocumentUrl,
            Instant signedAt,
            Instant effectiveFrom,
            Instant expirationDate,
            @Size(max = 4000) String securityMeasures,
            @Min(1) @Max(720) int breachNotificationCommitmentHours,
            boolean auditRights,
            @Size(max = 4000) String auditRightsNotes,
            @Size(max = 4000) String dataReturnOrDeletionTerms,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 250) String processorName,
            @Size(max = 250) String processorLegalEntity,
            @Size(max = 250) String processorContact,
            @Size(max = 250) String processorDpoContact,
            @Size(min = 2, max = 2)
            @Pattern(regexp = "^[A-Za-z]{2}$") String processorCountry,
            @NotBlank @Size(max = 4000) String servicesDescription,
            Set<String> subProcessorCategories,
            Set<UUID> linkedProcessingActivityIds,
            Set<String> thirdCountryTransfers,
            @Size(max = 4000) String transferSafeguards,
            @Size(max = 1024) String contractDocumentUrl,
            Instant signedAt,
            Instant effectiveFrom,
            Instant expirationDate,
            @Size(max = 4000) String securityMeasures,
            @Min(1) @Max(720) int breachNotificationCommitmentHours,
            boolean auditRights,
            @Size(max = 4000) String auditRightsNotes,
            @Size(max = 4000) String dataReturnOrDeletionTerms) {}

    public record TerminateRequest(@NotBlank @Size(max = 2000) String reason) {}
}
