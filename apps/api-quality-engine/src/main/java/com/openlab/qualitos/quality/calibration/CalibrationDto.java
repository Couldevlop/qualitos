package com.openlab.qualitos.quality.calibration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class CalibrationDto {

    private CalibrationDto() {}

    // ----- Equipment -----

    public record CreateEquipmentRequest(
            @NotBlank @Size(max = 120)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._\\-]{1,119}$",
                    message = "code must be alphanumeric kebab/snake (2..120 chars)")
            String code,
            @NotBlank @Size(max = 250) String name,
            @Size(max = 200) String manufacturer,
            @Size(max = 200) String model,
            @Size(max = 200) String serialNumber,
            @Size(max = 500) String location,
            boolean critical,
            UUID iotDeviceId,
            UUID ownerUserId,
            @NotNull UUID createdBy
    ) {}

    public record UpdateEquipmentRequest(
            @Size(max = 250) String name,
            @Size(max = 200) String manufacturer,
            @Size(max = 200) String model,
            @Size(max = 200) String serialNumber,
            @Size(max = 500) String location,
            Boolean critical,
            UUID iotDeviceId,
            UUID ownerUserId
    ) {}

    public record EquipmentResponse(
            UUID id, UUID tenantId, String code, String name,
            String manufacturer, String model, String serialNumber,
            String location, EquipmentStatus status, boolean critical,
            UUID iotDeviceId, UUID ownerUserId,
            UUID createdBy, Instant createdAt, Instant updatedAt
    ) {}

    // ----- Plan -----

    public record UpsertPlanRequest(
            @Min(1) @Max(120) @NotNull Integer frequencyMonths,
            @Size(max = 500) String procedureReference,
            @Size(max = 500) String tolerance,
            @Size(max = 250) String accreditationRef,
            /** Date initiale de prochaine calibration (avant le premier record). */
            @NotNull LocalDate firstDueOn
    ) {}

    public record PlanResponse(
            UUID id, UUID tenantId, UUID equipmentId,
            int frequencyMonths, String procedureReference,
            String tolerance, String accreditationRef,
            LocalDate lastCalibratedOn, LocalDate nextDueOn,
            boolean overdue,
            Instant createdAt, Instant updatedAt
    ) {}

    // ----- Records -----

    public record CreateRecordRequest(
            @NotNull LocalDate performedOn,
            UUID performedByUserId,
            @Size(max = 250) String performedByOrg,
            @NotNull CalibrationResult result,
            @Size(max = 4000) String measurements,
            @Size(max = 250) String certificateReference,
            /** Si renseigné, écrase le nextDueOn dérivé du plan. */
            LocalDate nextDueOverride
    ) {}

    public record RecordResponse(
            UUID id, UUID tenantId, UUID equipmentId,
            LocalDate performedOn, UUID performedByUserId, String performedByOrg,
            CalibrationResult result, String measurements,
            String certificateReference, LocalDate nextDueOverride,
            Instant createdAt
    ) {}

    // ----- MSA -----

    public record CreateMsaRequest(
            @NotNull MsaType type,
            @NotNull LocalDate performedOn,
            @NotNull BigDecimal studyValue,
            BigDecimal passingThreshold,
            @NotNull MsaResult result,
            @Size(max = 2000) String notes,
            @NotNull UUID createdBy
    ) {}

    public record MsaResponse(
            UUID id, UUID tenantId, UUID equipmentId,
            MsaType type, LocalDate performedOn,
            BigDecimal studyValue, BigDecimal passingThreshold,
            MsaResult result, String notes,
            UUID createdBy, Instant createdAt
    ) {}

    // ----- Summary / overdue -----

    public record EquipmentSummary(
            UUID equipmentId,
            EquipmentStatus status,
            boolean critical,
            LocalDate lastCalibratedOn,
            LocalDate nextDueOn,
            boolean overdue,
            CalibrationResult lastResult,
            long passRecords,
            long failRecords,
            long conditionalRecords,
            long msaPass,
            long msaFail
    ) {}
}
