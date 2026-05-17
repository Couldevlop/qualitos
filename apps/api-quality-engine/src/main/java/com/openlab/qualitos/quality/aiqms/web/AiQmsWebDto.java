package com.openlab.qualitos.quality.aiqms.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public final class AiQmsWebDto {

    private AiQmsWebDto() {}

    public record DraftRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotBlank @Size(max = 32)
            @Pattern(regexp = "^\\d+\\.\\d+(\\.\\d+)?$",
                    message = "version must match X.Y or X.Y.Z")
            String version,
            @NotBlank @Size(max = 250) String name,
            @Size(max = 4000) String description,
            @Size(max = 8000) String regulatoryComplianceStrategy,
            @Size(max = 8000) String designControlDescription,
            @Size(max = 8000) String qualityControlDescription,
            @Size(max = 8000) String dataManagementDescription,
            @Size(max = 8000) String riskManagementDescription,
            @Size(max = 8000) String pmmDescription,
            @Size(max = 8000) String regulatorCommunicationDescription,
            @Size(max = 8000) String resourceManagementDescription,
            @Size(max = 8000) String supplierMonitoringDescription,
            Set<UUID> coveredAiSystemIds,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 250) String name,
            @Size(max = 4000) String description,
            @Size(max = 8000) String regulatoryComplianceStrategy,
            @Size(max = 8000) String designControlDescription,
            @Size(max = 8000) String qualityControlDescription,
            @Size(max = 8000) String dataManagementDescription,
            @Size(max = 8000) String riskManagementDescription,
            @Size(max = 8000) String pmmDescription,
            @Size(max = 8000) String regulatorCommunicationDescription,
            @Size(max = 8000) String resourceManagementDescription,
            @Size(max = 8000) String supplierMonitoringDescription,
            Set<UUID> coveredAiSystemIds) {}

    public record ApproveRequest(
            @NotNull UUID submittedByUserId,
            @NotNull UUID approvedByUserId,
            @Size(max = 4000) String approvalNotes) {}

    public record SupersedeRequest(@NotNull UUID supersededByQmsId) {}

    public record ArchiveRequest(@NotBlank @Size(max = 2000) String reason) {}
}
