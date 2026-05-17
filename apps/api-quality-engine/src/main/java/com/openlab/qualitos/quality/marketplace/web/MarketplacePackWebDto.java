package com.openlab.qualitos.quality.marketplace.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class MarketplacePackWebDto {
    private MarketplacePackWebDto() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 64) @Pattern(regexp = "^[a-z][a-z0-9_-]{1,63}$") String packId,
            @NotBlank @Size(max = 32) @Pattern(regexp = "^\\d+\\.\\d+(\\.\\d+)?$") String version,
            @NotBlank @Size(max = 120) String publisher,
            @NotBlank @Size(max = 250) String title,
            @Size(max = 4000) String description,
            @NotBlank @Size(max = 80) String sector,
            @PositiveOrZero int priceCents,
            @NotBlank @Pattern(regexp = "^(EUR|USD|GBP|CHF|JPY)$") String currency,
            @NotBlank @Size(max = 2000) String manifestUrl,
            @Size(max = 128) String signatureHash
    ) {}
}
