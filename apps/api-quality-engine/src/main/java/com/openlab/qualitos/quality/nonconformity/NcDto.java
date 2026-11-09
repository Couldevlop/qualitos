package com.openlab.qualitos.quality.nonconformity;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
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
            /**
             * La nature fine de l'écart, sous sa catégorie : le code d'une entrée
             * du catalogue du client. Facultative — un écart se saisit d'abord et
             * se classe finement ensuite. Un code inconnu, ou rattaché à une autre
             * catégorie, est refusé (400).
             */
            @Size(max = 30) String subcategoryCode,
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
            /**
             * Absent = inchangé, comme tout le reste de cette requête partielle.
             * La chaîne vide EFFACE le classement fin : reclasser un écart dont on
             * a compris qu'il relevait d'autre chose doit être possible.
             */
            @Size(max = 30) String subcategoryCode,
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
    /**
     * Le rejet d'une réclamation externe.
     *
     * <p>Le motif est obligatoire : écarter une réclamation sans dire pourquoi est
     * indéfendable devant le client comme devant l'auditeur.
     */
    public record RejectRequest(
            @NotBlank @Size(max = 2000) String reason) {}

    /**
     * Ouvrir, depuis cet écart, le cycle ou le projet qui le traitera (§3.6).
     *
     * <p>{@code method} peut rester nulle : la gravité donne alors la méthode
     * ({@link NcMethod#fromSeverity}), ce qui est le cas courant. La renseigner
     * est un choix explicite de la personne qui tient le dossier — un écart
     * mineur sur un procédé déjà sous carte de contrôle peut mériter un DMAIC.
     *
     * <p>{@code pilotId} n'est pas facultatif, et c'est le point dur : un cycle
     * ou un projet sans personne qui en répond ne se revoit jamais. L'écran le
     * choisit dans l'annuaire, jamais en texte libre.
     *
     * @param method   la méthode voulue, ou {@code null} pour celle de la gravité
     * @param pilotId  le compte qui mène le cycle (propriétaire) ou le projet (black belt)
     * @param dueDate  l'échéance visée ; ignorée pour un cycle PDCA, qui n'en porte pas
     */
    public record ConvertRequest(
            NcMethod method,
            @NotNull(message = "Pilot is required") UUID pilotId,
            LocalDate dueDate
    ) {}

    public record Response(
            UUID id,
            UUID tenantId,
            String reference,
            String title,
            String description,
            NcCategory category,
            /** Le code de la nature fine ; l'écran le traduit par le catalogue. */
            String subcategoryCode,
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
            /** Le cycle PDCA ouvert depuis cet écart, s'il l'a été (§3.6). */
            UUID pdcaCycleId,
            /** Le projet DMAIC ouvert depuis cet écart, s'il l'a été (§3.6). */
            UUID dmaicProjectId,
            String rootCause,
            String resolutionNote,
            Instant resolvedAt,
            Instant closedAt,
            /** Pourquoi la réclamation a été écartée — ce que le client vient lire. */
            String rejectionReason,
            Instant rejectedAt,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /**
     * Dénombrements affichés en tuiles au-dessus d'une liste de NC.
     *
     * <p>{@code origin} rappelle SUR QUOI portent ces nombres : la même liste
     * existe en interne, en externe et toutes origines confondues, et un total
     * sans son périmètre se lit de travers. {@code null} = les deux origines.
     *
     * <p>{@code cancelled} n'a de sens que sur les écarts signalés du dehors —
     * on n'annule pas un constat qu'on a fait soi-même, on le résout ou on le
     * clôt. L'écran interne ne l'affiche pas ; le champ reste rendu pour que
     * l'API ne dépende pas de qui la consomme.
     */
    public record NcStatistics(
            UUID tenantId,
            NcOrigin origin,
            long total,
            long open,
            long underAnalysis,
            long actionDefined,
            long resolved,
            long closed,
            long cancelled,
            /** Rejetées — toujours 0 sur l'écran interne, qui ne rejette pas. */
            long rejected) {}
}
