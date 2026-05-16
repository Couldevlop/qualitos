package com.openlab.qualitos.quality.automateddecisions.web;

import com.openlab.qualitos.quality.automateddecisions.domain.Art22LawfulBasis;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public final class AutomatedDecisionWebDto {

    private AutomatedDecisionWebDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotBlank @Size(max = 250) String name,
            @Size(max = 4000) String description,
            @NotNull AutomatedDecisionType decisionType,
            Art22LawfulBasis art22LawfulBasis,
            @Size(max = 4000) String lawfulBasisDetails,
            Set<String> inputDataCategories,
            Set<UUID> linkedProcessingActivityIds,
            UUID linkedDpiaId,
            @Size(max = 8000) String algorithmDescription,
            @Size(max = 4000) String significanceForSubject,
            @Size(max = 4000) String humanReviewMechanism,
            @Size(max = 4000) String objectionMechanism,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 250) String name,
            @Size(max = 4000) String description,
            @NotNull AutomatedDecisionType decisionType,
            Art22LawfulBasis art22LawfulBasis,
            @Size(max = 4000) String lawfulBasisDetails,
            Set<String> inputDataCategories,
            Set<UUID> linkedProcessingActivityIds,
            UUID linkedDpiaId,
            @Size(max = 8000) String algorithmDescription,
            @Size(max = 4000) String significanceForSubject,
            @Size(max = 4000) String humanReviewMechanism,
            @Size(max = 4000) String objectionMechanism) {}
}
