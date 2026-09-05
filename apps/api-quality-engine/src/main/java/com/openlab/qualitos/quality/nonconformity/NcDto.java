package com.openlab.qualitos.quality.nonconformity;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class NcDto {

    private NcDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 255) String title,
            /**
             * Obligatoire. Un titre seul (« Défaut peinture ») ne dit ni ce qui a
             * été constaté, ni où, ni ce que ça a produit : six mois plus tard,
             * personne ne peut plus instruire l'écart, et l'analyse de cause
             * racine part de rien.
             *
             * <p>Exigée ICI et pas seulement dans le formulaire : l'API est
             * publique (§13.1) et l'application mobile rejoue sa file hors-ligne
             * par le même chemin. Une règle qui ne vit que dans un écran n'est
             * pas une règle.
             */
            @NotBlank String description,
            @NotNull NcCategory category,
            @NotNull NcSeverity severity,
            @NotNull Instant detectedAt,
            @Size(max = 255) String zone,
            @DecimalMin("-90.0") @DecimalMax("90.0") Double geoLat,
            @DecimalMin("-180.0") @DecimalMax("180.0") Double geoLng,
            String photoUrls,
            UUID reporterId,
            /** Interne par défaut : voir {@link NcOrigin#orDefault}. */
            NcOrigin origin,
            UUID productId,
            UUID fmeaItemId
    ) {}

    public record UpdateRequest(
            @Size(max = 255) String title,
            /**
             * « Non blanche SI fournie » : la mise à jour est PARTIELLE — un champ
             * absent vaut « inchangé » (voir {@code NcService.update}). Un
             * {@code @NotBlank} rejetterait donc toute modification qui ne
             * renvoie pas la description, c'est-à-dire presque toutes.
             *
             * <p>{@code @Pattern} laisse passer {@code null} par contrat Jakarta,
             * et refuse la chaîne vide ou blanche : on ne peut pas EFFACER une
             * description déjà écrite, ce qui reviendrait à contourner
             * l'obligation posée à la création. {@code (?s)} pour que le point
             * couvre les sauts de ligne d'un constat sur plusieurs lignes.
             */
            @Pattern(regexp = "(?s).*\\S.*",
                     message = "description must not be blank when provided")
            String description,
            NcCategory category,
            NcSeverity severity,
            @Size(max = 255) String zone,
            @DecimalMin("-90.0") @DecimalMax("90.0") Double geoLat,
            @DecimalMin("-180.0") @DecimalMax("180.0") Double geoLng,
            String photoUrls,
            NcOrigin origin,
            UUID productId,
            UUID fmeaItemId
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
            NcOrigin origin,
            Instant detectedAt,
            String zone,
            Double geoLat,
            Double geoLng,
            String photoUrls,
            UUID reporterId,
            /** Nom du signalant, figé à la création (dérivé du JWT, jamais du body). */
            String reporterName,
            UUID productId,
            UUID fmeaItemId,
            UUID capaCaseId,
            String rootCause,
            String resolutionNote,
            Instant resolvedAt,
            Instant closedAt,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
