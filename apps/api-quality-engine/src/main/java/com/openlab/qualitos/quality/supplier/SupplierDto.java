package com.openlab.qualitos.quality.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class SupplierDto {

    private SupplierDto() {}

    public record CreateSupplierRequest(
            @NotBlank @Size(max = 120)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._\\-]{0,119}$",
                    message = "code must be alphanumeric kebab/snake (max 120 chars)")
            String code,
            @NotBlank @Size(max = 250) String name,
            @Pattern(regexp = "^[A-Z]{2}$", message = "countryCode must be ISO-3166 alpha-2")
            String countryCode,
            @Email @Size(max = 320) String contactEmail,
            @NotNull SupplierType supplierType,
            @NotNull UUID createdBy
    ) {}

    public record UpdateSupplierRequest(
            @Size(max = 250) String name,
            @Pattern(regexp = "^[A-Z]{2}$") String countryCode,
            @Email @Size(max = 320) String contactEmail,
            SupplierType supplierType
    ) {}

    public record SupplierResponse(
            UUID id,
            UUID tenantId,
            String code,
            String name,
            String countryCode,
            String contactEmail,
            SupplierType supplierType,
            SupplierStatus status,
            int score,
            LocalDate lastAuditAt,
            Instant approvedAt,
            UUID approvedBy,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record StatusChangeRequest(
            @NotNull UUID actorUserId,
            @Size(max = 1000) String reason
    ) {}

    public record CreateAuditRequest(
            @NotNull LocalDate auditedOn,
            @Min(0) @Max(100) @NotNull Integer score,
            UUID auditorUserId,
            @Size(max = 2000) String findingsSummary,
            @Min(0) Integer criticalFindingsCount,
            @Min(0) Integer majorFindingsCount,
            @Min(0) Integer minorFindingsCount
    ) {}

    public record AuditResponse(
            UUID id,
            UUID tenantId,
            UUID supplierId,
            LocalDate auditedOn,
            int score,
            UUID auditorUserId,
            String findingsSummary,
            int criticalFindingsCount,
            int majorFindingsCount,
            int minorFindingsCount,
            Instant createdAt
    ) {}

    public record CreateNonConformityRequest(
            @Size(max = 100) String lotReference,
            @Size(max = 1000) String description,
            @NotNull NonConformitySeverity severity,
            @NotNull LocalDate detectedOn
    ) {}

    public record UpdateNonConformityRequest(
            String lotReference,
            String description,
            NonConformitySeverity severity,
            NonConformityStatus status,
            LocalDate resolvedOn,
            String resolution
    ) {}

    public record NonConformityResponse(
            UUID id,
            UUID tenantId,
            UUID supplierId,
            String lotReference,
            String description,
            NonConformitySeverity severity,
            NonConformityStatus status,
            LocalDate detectedOn,
            LocalDate resolvedOn,
            String resolution,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record CreateCertificateRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[a-z0-9][a-z0-9_-]{1,62}$") String standardCode,
            @Size(max = 200) String reference,
            @NotNull LocalDate issuedOn,
            @NotNull LocalDate expiresOn,
            @Size(max = 1024) String documentUrl
    ) {}

    public record CertificateResponse(
            UUID id,
            UUID tenantId,
            UUID supplierId,
            String standardCode,
            String reference,
            LocalDate issuedOn,
            LocalDate expiresOn,
            String documentUrl,
            boolean expired,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record SupplierStatistics(
            UUID supplierId,
            int score,
            SupplierStatus status,
            long openNonConformities,
            long resolvedNonConformitiesRecent,
            long expiredCertificates,
            LocalDate lastAuditAt
    ) {}
}
