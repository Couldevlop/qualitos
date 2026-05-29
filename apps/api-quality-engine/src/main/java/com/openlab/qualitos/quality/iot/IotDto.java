package com.openlab.qualitos.quality.iot;

import com.openlab.qualitos.quality.capa.CapaCriticity;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class IotDto {

    private IotDto() {}

    public record CreateDeviceRequest(
            @NotBlank @Size(max = 120)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._\\-]{0,119}$",
                    message = "code must be alphanumeric kebab/snake (max 120 chars)")
            String code,
            @NotBlank @Size(max = 200) String name,
            @NotNull IotDeviceType deviceType,
            @NotNull IotProtocol protocol,
            @Size(max = 500) String location,
            @Size(max = 1000) String description,
            String metadataJson,
            @NotNull UUID createdBy
    ) {}

    public record UpdateDeviceRequest(
            @Size(max = 200) String name,
            IotDeviceType deviceType,
            IotProtocol protocol,
            @Size(max = 500) String location,
            @Size(max = 1000) String description,
            String metadataJson
    ) {}

    public record DeviceResponse(
            UUID id,
            UUID tenantId,
            String code,
            String name,
            IotDeviceType deviceType,
            IotProtocol protocol,
            IotDeviceStatus status,
            String location,
            String description,
            String metadataJson,
            Instant lastSeenAt,
            long telemetryCount,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record TelemetryIngestRequest(
            @NotBlank @Size(max = 100) String metric,
            BigDecimal valueNumeric,
            @Size(max = 500) String valueText,
            @Size(max = 32) String unit,
            /** Si null, on prend Instant.now(). */
            Instant recordedAt,
            /** Si null, MANUAL (cas REST direct). */
            IotProtocol source
    ) {}

    public record TelemetryResponse(
            UUID id,
            UUID tenantId,
            UUID deviceId,
            String metric,
            BigDecimal valueNumeric,
            String valueText,
            String unit,
            IotProtocol source,
            Instant recordedAt,
            Instant ingestedAt
    ) {}

    // ---- Thresholds (§9.7, §9.9) ----

    public record ThresholdRequest(
            /** Null = seuil applicable à tous les équipements du tenant pour la métrique. */
            UUID deviceId,
            @NotBlank @Size(max = 100) String metric,
            Double minValue,
            Double maxValue,
            @NotNull CapaCriticity capaCriticity,
            @NotNull UUID capaOwnerId,
            /** Si null à la création, le seuil est activé par défaut. */
            Boolean enabled
    ) {
        @AssertTrue(message = "au moins une borne (minValue ou maxValue) doit être définie")
        public boolean isBoundsDefined() {
            return minValue != null || maxValue != null;
        }

        @AssertTrue(message = "minValue doit être <= maxValue")
        public boolean isRangeConsistent() {
            return minValue == null || maxValue == null || minValue <= maxValue;
        }
    }

    public record ThresholdResponse(
            UUID id,
            UUID tenantId,
            UUID deviceId,
            String metric,
            Double minValue,
            Double maxValue,
            CapaCriticity capaCriticity,
            UUID capaOwnerId,
            boolean enabled,
            Instant createdAt
    ) {}
}
