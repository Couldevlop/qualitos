package com.openlab.qualitos.quality.marketplace.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTOs entrants (validation Jakarta — OWASP A03/A04). Aucun champ d'acteur
 * (submittedBy/installedBy) : ils sont dérivés du JWT côté service (A01).
 */
public final class MarketplacePackWebDto {
    private MarketplacePackWebDto() {}

    /** Soumission d'un pack par un partenaire. */
    public record SubmitRequest(
            @NotBlank @Size(max = 64) @Pattern(regexp = "^[a-z][a-z0-9_-]{1,63}$") String packId,
            @NotBlank @Size(max = 32) @Pattern(regexp = "^\\d+\\.\\d+(\\.\\d+)?$") String version,
            @NotBlank @Size(max = 120) String publisher,
            @NotBlank @Size(max = 250) String title,
            @Size(max = 4000) String description,
            @NotBlank @Size(max = 80) String sector,
            @Size(max = 30) List<@Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") String> norms,
            @PositiveOrZero int priceCents,
            @NotBlank @Pattern(regexp = "^(EUR|USD|GBP|CHF|JPY)$") String currency,
            @NotBlank @Size(max = 2000) String manifestUrl,
            @NotBlank @Size(max = 65536) String manifestJson,
            @NotBlank @Size(min = 16, max = 128) String signatureHash
    ) {}

    /** Rejet motivé par l'éditeur. */
    public record RejectRequest(
            @NotBlank @Size(max = 2000) String reason
    ) {}

    /** Notation (1..5) par un tenant ayant installé. */
    public record RateRequest(
            @Min(1) @Max(5) int stars
    ) {}
}
