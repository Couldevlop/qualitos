package com.openlab.qualitos.quality.ehrconnector;

import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class EhrDto {

    private EhrDto() {}

    public record CreateConnectionRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull EhrProvider provider,
            @NotBlank @Size(max = 512)
            @Pattern(regexp = "^https://.+", message = "fhirBaseUrl must be https://")
            String fhirBaseUrl,
            @NotNull EhrAuthMode authMode,
            @Size(max = 200) String username,
            @NotBlank @Size(min = 4, max = 1024) String secret,
            @Size(max = 120) String resourceCategory,
            @NotNull UUID createdBy
    ) {}

    public record UpdateConnectionRequest(
            @Size(max = 120) String name,
            @Size(max = 512)
            @Pattern(regexp = "^https://.+", message = "fhirBaseUrl must be https://")
            String fhirBaseUrl,
            EhrAuthMode authMode,
            @Size(max = 200) String username,
            @Size(min = 4, max = 1024) String secret,
            @Size(max = 120) String resourceCategory,
            ConnectionStatus status
    ) {}

    /** Réponse connexion : n'expose AUCUN champ sensible (ni secret, ni ciphertext). */
    public record ConnectionResponse(
            UUID id,
            UUID tenantId,
            String name,
            EhrProvider provider,
            String fhirBaseUrl,
            EhrAuthMode authMode,
            String username,
            String resourceCategory,
            ConnectionStatus status,
            int consecutiveFailures,
            Instant lastSyncAt,
            Instant lastSuccessAt,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /** Rapport de synchronisation : compteurs + éventuel message d'erreur (jamais de PII). */
    public record SyncReport(
            UUID connectionId,
            int totalFetched,
            int created,
            int skipped,
            int errors,
            Instant ranAt,
            String errorMessage
    ) {}
}
