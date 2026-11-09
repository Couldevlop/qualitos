package com.openlab.qualitos.quality.dmaic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DmaicDto {

    private DmaicDto() {}

    public record CreateProjectRequest(
            @NotBlank @Size(max = 255) String title,
            String problemStatement,
            String goalStatement,
            @NotNull UUID blackBeltId,
            UUID championId,
            LocalDate targetCompletionDate,
            Double specLowerLimit,
            Double specUpperLimit,
            Double specTarget,
            @Size(max = 50) String specUnit,
            Double estimatedSavingsEur
    ) {}

    public record UpdateProjectRequest(
            @Size(max = 255) String title,
            String problemStatement,
            String goalStatement,
            UUID blackBeltId,
            UUID championId,
            LocalDate targetCompletionDate,
            Double specLowerLimit,
            Double specUpperLimit,
            Double specTarget,
            @Size(max = 50) String specUnit,
            Double estimatedSavingsEur
    ) {}

    public record AddMeasureRequest(
            // Facultative : un relevé peut être noté avant que sa valeur soit connue.
            Double value,
            @Size(max = 100) String subgroupId,
            @Size(max = 255) String sourceRef,
            Instant recordedAt,
            UUID operatorId,
            String note
    ) {}

    public record AssignPokaYokeRequest(
            @NotNull UUID deviceId,
            String note
    ) {}

    public record UpdateAssignmentRequest(
            PokaYokeAssignmentStatus status,
            String note,
            Double defectReductionPct
    ) {}

    public record ProjectResponse(
            UUID id,
            UUID tenantId,
            String title,
            String problemStatement,
            String goalStatement,
            DmaicPhase phase,
            DmaicStatus status,
            UUID championId,
            UUID blackBeltId,
            LocalDate targetCompletionDate,
            Double specLowerLimit,
            Double specUpperLimit,
            Double specTarget,
            String specUnit,
            Double estimatedSavingsEur,
            int measureCount,
            int pokaYokeCount,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record MeasureResponse(
            UUID id,
            UUID projectId,
            Double value,
            String subgroupId,
            String sourceRef,
            Instant recordedAt,
            UUID operatorId,
            String note,
            Instant createdAt
    ) {}

    /**
     * @param own vrai quand le dispositif appartient au client, faux quand il
     *            vient du catalogue partage de la plateforme. L'ecran le dit :
     *            un detrompeur maison ne se lit pas comme une reference.
     */
    public record DeviceSummary(
            UUID id,
            String code,
            String name,
            PokaYokeType type,
            PokaYokeMechanism mechanism,
            String applicableIndustries,
            String implementationCost,
            boolean own,
            String sourceReference
    ) {}

    /**
     * Inscrire un detrompeur a la bibliotheque du client.
     *
     * <p>Le code n'est pas demande : il est attribue par le serveur, parce qu'il
     * doit rester unique a l'echelle de la plateforme, ou cohabitent le
     * catalogue partage et les bibliotheques de chaque client.
     *
     * @param sourceReference d'ou vient le dispositif, cite en clair. Exige :
     *                        la promesse est qu'il naisse d'une action, et un
     *                        dispositif sans origine n'est pas reutilisable en
     *                        confiance.
     * @param projectId       le projet sur lequel le deployer tout de suite,
     *                        ou {@code null} pour l'inscrire seulement.
     */
    public record CreateDeviceRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 4000) String description,
            @NotNull PokaYokeType type,
            @NotNull PokaYokeMechanism mechanism,
            @Size(max = 500) String applicableIndustries,
            @Size(max = 4000) String examples,
            @Size(max = 20) String implementationCost,
            @NotBlank @Size(max = 255) String sourceReference,
            UUID projectId
    ) {}

    public record DeviceDetail(
            UUID id,
            String code,
            String name,
            String description,
            PokaYokeType type,
            PokaYokeMechanism mechanism,
            String applicableIndustries,
            String examples,
            String implementationCost,
            boolean own,
            String sourceReference,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record AssignmentResponse(
            UUID id,
            UUID projectId,
            UUID deviceId,
            String deviceCode,
            String deviceName,
            PokaYokeType deviceType,
            PokaYokeAssignmentStatus status,
            String note,
            Instant implementedAt,
            Instant verifiedAt,
            Double defectReductionPct,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /**
     * Resultat des indices de capabilite Cp/Cpk + statistiques descriptives.
     * Calcule a partir des mesures non groupees ou des sous-groupes.
     */
    public record CapabilityResponse(
            int sampleSize,
            Double mean,
            Double stdDev,
            Double min,
            Double max,
            Double specLowerLimit,
            Double specUpperLimit,
            Double specTarget,
            Double cp,         // (USL - LSL) / (6 * sigma)
            Double cpk,        // min((USL - mean)/(3*sigma), (mean - LSL)/(3*sigma))
            Double cpu,        // (USL - mean) / (3 * sigma)
            Double cpl,        // (mean - LSL) / (3 * sigma)
            Double sigmaLevel, // niveau Sigma
            String interpretation,
            List<String> warnings
    ) {}
}
