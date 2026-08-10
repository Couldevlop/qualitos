package com.openlab.qualitos.quality.ishikawa;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class IshikawaDto {

    private IshikawaDto() {}

    public record CreateDiagramRequest(
            @NotBlank(message = "Problem statement is required")
            @Size(max = 500, message = "Problem statement cannot exceed 500 characters")
            String problemStatement,
            String description,
            IshikawaMode mode,
            @NotNull(message = "Owner ID is required") UUID ownerId,
            /**
             * Non-conformité d'où part le diagramme. Facultative : un Ishikawa
             * se tient aussi pour lui-même. Le tenant, lui, vient toujours du
             * jeton — jamais du corps (§18.2 #2).
             */
            UUID ncId
    ) {}

    public record UpdateDiagramRequest(
            @Size(max = 500, message = "Problem statement cannot exceed 500 characters")
            String problemStatement,
            String description,
            IshikawaMode mode,
            IshikawaStatus status
    ) {}

    public record CauseRequest(
            @NotNull(message = "Category is required") CauseCategory category,
            @NotBlank(message = "Label is required")
            @Size(max = 500, message = "Label cannot exceed 500 characters") String label,
            String description,
            UUID parentId,
            @DecimalMin(value = "0.0", message = "rootCauseScore must be >= 0")
            @DecimalMax(value = "1.0", message = "rootCauseScore must be <= 1")
            Double rootCauseScore
    ) {}

    public record UpdateCauseRequest(
            CauseCategory category,
            @Size(max = 500, message = "Label cannot exceed 500 characters") String label,
            String description,
            @DecimalMin(value = "0.0", message = "rootCauseScore must be >= 0")
            @DecimalMax(value = "1.0", message = "rootCauseScore must be <= 1")
            Double rootCauseScore
    ) {}

    public record DiagramResponse(
            UUID id,
            UUID tenantId,
            String problemStatement,
            String description,
            IshikawaMode mode,
            IshikawaStatus status,
            UUID ownerId,
            /** Non-conformité d'origine, ou null : le diagramme se tient seul. */
            UUID ncId,
            Instant createdAt,
            Instant updatedAt,
            List<CauseResponse> causes
    ) {}

    public record CauseResponse(
            UUID id,
            UUID diagramId,
            UUID parentId,
            CauseCategory category,
            String label,
            String description,
            Double rootCauseScore,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /** Cause probable suggérée par l'IA (à valider/ajouter par l'utilisateur). §3.5 */
    public record SuggestedCause(
            CauseCategory category,
            String label,
            String rationale
    ) {}
    // ---- Plan d'actions (§3.5) ------------------------------------------------

    /** Une action décidée devant le diagramme : quoi, par qui, décidée quand. */
    public record ActionResponse(
            UUID id,
            UUID diagramId,
            String label,
            String responsible,
            LocalDate decidedOn,
            IshikawaActionStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record CreateActionRequest(
            @NotBlank @Size(max = 500) String label,
            @Size(max = 255) String responsible,
            /** Date de la DÉCISION (réunion, comité), pas une échéance. */
            LocalDate decidedOn,
            /** Absent = « à faire ». */
            IshikawaActionStatus status
    ) {}

    /** Modification cellule par cellule : un champ absent signifie « inchangé ». */
    public record UpdateActionRequest(
            @Size(max = 500) String label,
            @Size(max = 255) String responsible,
            LocalDate decidedOn,
            IshikawaActionStatus status
    ) {}
}