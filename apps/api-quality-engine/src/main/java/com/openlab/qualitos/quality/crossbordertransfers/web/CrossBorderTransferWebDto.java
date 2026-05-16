package com.openlab.qualitos.quality.crossbordertransfers.web;

import com.openlab.qualitos.quality.crossbordertransfers.domain.TransferMechanism;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public final class CrossBorderTransferWebDto {

    private CrossBorderTransferWebDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotBlank @Size(max = 250) String recipientName,
            @Size(max = 250) String recipientLegalEntity,
            @Size(max = 250) String recipientContact,
            Set<String> destinationCountries,
            @NotNull TransferMechanism mechanism,
            @Size(max = 4000) String safeguardsDescription,
            @Size(max = 1024) String safeguardsDocumentUrl,
            @Size(max = 4000) String derogationJustification,
            Set<String> dataCategories,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedProcessorAgreementIds,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 250) String recipientName,
            @Size(max = 250) String recipientLegalEntity,
            @Size(max = 250) String recipientContact,
            Set<String> destinationCountries,
            @NotNull TransferMechanism mechanism,
            @Size(max = 4000) String safeguardsDescription,
            @Size(max = 1024) String safeguardsDocumentUrl,
            @Size(max = 4000) String derogationJustification,
            Set<String> dataCategories,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedProcessorAgreementIds) {}

    public record SuspendRequest(@NotBlank @Size(max = 2000) String reason) {}
    public record TerminateRequest(@NotBlank @Size(max = 2000) String reason) {}
}
