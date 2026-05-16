package com.openlab.qualitos.quality.retention.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.util.UUID;

public final class RetentionRuleWebDto {

    private RetentionRuleWebDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[a-z][a-z0-9._-]{1,63}$",
                    message = "dataCategoryCode must match [a-z][a-z0-9._-]{1,63}")
            String dataCategoryCode,
            @Size(max = 250) String dataCategoryLabel,
            @NotNull Duration retentionPeriod,
            @NotBlank @Size(max = 2000) String legalBasis,
            @Size(max = 1024) String lawfulBasisReference,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @Size(max = 250) String dataCategoryLabel,
            @NotNull Duration retentionPeriod,
            @NotBlank @Size(max = 2000) String legalBasis,
            @Size(max = 1024) String lawfulBasisReference) {}
}
