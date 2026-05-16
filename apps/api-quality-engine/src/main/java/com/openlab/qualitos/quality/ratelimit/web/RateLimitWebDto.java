package com.openlab.qualitos.quality.ratelimit.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class RateLimitWebDto {

    private RateLimitWebDto() {}

    public record UpsertRequest(
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "^[a-z][a-z0-9._:-]{0,99}$") String scope,
            @Min(1) @Max(86400) int windowSeconds,
            @Min(1) @Max(1_000_000) int maxRequests,
            boolean enabled
    ) {}
}
