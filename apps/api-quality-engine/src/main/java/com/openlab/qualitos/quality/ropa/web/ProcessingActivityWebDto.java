package com.openlab.qualitos.quality.ropa.web;

import com.openlab.qualitos.quality.ropa.domain.LawfulBasis;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public final class ProcessingActivityWebDto {

    private ProcessingActivityWebDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotBlank @Size(max = 250) String name,
            @NotBlank @Size(max = 4000) String purposes,
            @NotNull LawfulBasis lawfulBasis,
            @Size(max = 4000) String lawfulBasisDetails,
            @NotBlank @Size(max = 250) String controllerName,
            @NotBlank @Size(max = 250) String controllerContact,
            @Size(max = 250) String dpoContact,
            @Size(max = 250) String jointControllerName,
            @Size(max = 250) String jointControllerContact,
            Set<String> dataSubjectCategories,
            Set<String> dataCategories,
            boolean specialCategoriesProcessed,
            @Size(max = 4000) String specialCategoriesJustification,
            Set<String> recipientCategories,
            Set<String> thirdCountryTransfers,
            @Size(max = 4000) String transferSafeguards,
            Set<UUID> linkedRetentionRuleIds,
            @Size(max = 4000) String technicalMeasures,
            @Size(max = 4000) String organizationalMeasures,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 250) String name,
            @NotBlank @Size(max = 4000) String purposes,
            @NotNull LawfulBasis lawfulBasis,
            @Size(max = 4000) String lawfulBasisDetails,
            @NotBlank @Size(max = 250) String controllerName,
            @NotBlank @Size(max = 250) String controllerContact,
            @Size(max = 250) String dpoContact,
            @Size(max = 250) String jointControllerName,
            @Size(max = 250) String jointControllerContact,
            Set<String> dataSubjectCategories,
            Set<String> dataCategories,
            boolean specialCategoriesProcessed,
            @Size(max = 4000) String specialCategoriesJustification,
            Set<String> recipientCategories,
            Set<String> thirdCountryTransfers,
            @Size(max = 4000) String transferSafeguards,
            Set<UUID> linkedRetentionRuleIds,
            @Size(max = 4000) String technicalMeasures,
            @Size(max = 4000) String organizationalMeasures) {}
}
