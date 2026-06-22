package com.openlab.qualitos.quality.dashboards.annotations.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class DashboardAnnotationWebDto {
    private DashboardAnnotationWebDto() {}

    /**
     * Annotation creation payload. NB: tenantId and authorId are NEVER accepted
     * here — they are taken from the validated JWT (§18.2 #2).
     */
    public record CreateRequest(
            @NotBlank
            @Size(max = 64)
            @Pattern(regexp = "^[a-z0-9]+(?:[._-][a-z0-9]+){0,5}$",
                    message = "chartKey must be dotted lowercase segments")
            String chartKey,

            @Size(max = 120) String anchorLabel,

            @NotBlank @Size(max = 2000) String body
    ) {}
}
