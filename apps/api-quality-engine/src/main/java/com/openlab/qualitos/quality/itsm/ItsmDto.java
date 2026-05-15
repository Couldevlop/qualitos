package com.openlab.qualitos.quality.itsm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ItsmDto {

    private ItsmDto() {}

    public record CreateConnectionRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull ItsmProvider provider,
            @NotBlank @Size(max = 512)
            @Pattern(regexp = "^https://.+", message = "baseUrl must be https://")
            String baseUrl,
            @Size(max = 200) String username,
            @NotBlank @Size(min = 4, max = 1024) String secret,
            @Size(max = 200) String externalScope,
            @NotNull UUID createdBy
    ) {}

    public record UpdateConnectionRequest(
            @Size(max = 120) String name,
            @Size(max = 512)
            @Pattern(regexp = "^https://.+", message = "baseUrl must be https://")
            String baseUrl,
            @Size(max = 200) String username,
            @Size(min = 4, max = 1024) String secret,
            @Size(max = 200) String externalScope,
            ConnectionStatus status
    ) {}

    public record ConnectionResponse(
            UUID id,
            UUID tenantId,
            String name,
            ItsmProvider provider,
            String baseUrl,
            String username,
            String externalScope,
            ConnectionStatus status,
            int consecutiveFailures,
            Instant lastSyncAt,
            Instant lastSuccessAt,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record SyncReport(
            UUID connectionId,
            int totalFetched,
            int newImports,
            int alreadyKnown,
            Instant ranAt,
            String errorMessage
    ) {}

    public record MappingResponse(
            UUID id,
            UUID tenantId,
            UUID connectionId,
            String externalId,
            String externalUrl,
            String externalStatus,
            String externalPriority,
            String externalTitle,
            String internalEntityType,
            UUID internalEntityId,
            Instant firstImportedAt,
            Instant lastSeenAt
    ) {}
}
