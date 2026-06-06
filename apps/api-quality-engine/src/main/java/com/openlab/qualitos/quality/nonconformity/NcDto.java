package com.openlab.qualitos.quality.nonconformity;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class NcDto {

    private NcDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            @NotNull NcCategory category,
            @NotNull NcSeverity severity,
            @NotNull Instant detectedAt,
            @Size(max = 255) String zone,
            @DecimalMin("-90.0") @DecimalMax("90.0") Double geoLat,
            @DecimalMin("-180.0") @DecimalMax("180.0") Double geoLng,
            String photoUrls,
            UUID reporterId
    ) {}

    public record UpdateRequest(
            @Size(max = 255) String title,
            String description,
            NcCategory category,
            NcSeverity severity,
            @Size(max = 255) String zone,
            @DecimalMin("-90.0") @DecimalMax("90.0") Double geoLat,
            @DecimalMin("-180.0") @DecimalMax("180.0") Double geoLng,
            String photoUrls
    ) {}

    /** Démarrage de l'analyse : la cause racine est optionnelle à ce stade. */
    public record StartAnalysisRequest(
            String rootCause
    ) {}

    /** Clôture de la résolution : la note de résolution est requise. */
    public record ResolveRequest(
            @NotBlank String resolutionNote
    ) {}

    /**
     * Escalade vers une CAPA. La NC ne porte pas toujours de responsable ;
     * la CAPA exige un owner (cf. CapaCase.ownerId NOT NULL) → fourni ici.
     */
    public record EscalateRequest(
            @NotNull UUID ownerId
    ) {}

    public record Response(
            UUID id,
            UUID tenantId,
            String reference,
            String title,
            String description,
            NcCategory category,
            NcSeverity severity,
            NcStatus status,
            Instant detectedAt,
            String zone,
            Double geoLat,
            Double geoLng,
            String photoUrls,
            UUID reporterId,
            UUID capaCaseId,
            String rootCause,
            String resolutionNote,
            Instant resolvedAt,
            Instant closedAt,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
