package com.openlab.qualitos.quality.dpoappointments.web;

import com.openlab.qualitos.quality.dpoappointments.domain.DpoType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class DpoAppointmentWebDto {

    private DpoAppointmentWebDto() {}

    public record ProposeRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotBlank @Size(max = 250) String dpoFullName,
            @NotBlank @Email @Size(max = 320) String dpoEmail,
            @Size(max = 64) String dpoPhone,
            @NotNull DpoType dpoType,
            @Size(max = 250) String externalCompanyName,
            @Size(max = 4000) String qualifications,
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{0,63}$") String scope,
            Set<UUID> linkedProcessingActivityIds,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 250) String dpoFullName,
            @NotBlank @Email @Size(max = 320) String dpoEmail,
            @Size(max = 64) String dpoPhone,
            @NotNull DpoType dpoType,
            @Size(max = 250) String externalCompanyName,
            @Size(max = 4000) String qualifications,
            Set<UUID> linkedProcessingActivityIds) {}

    public record ActivateRequest(
            @NotNull Instant effectiveFrom,
            @NotNull Instant regulatorNotifiedAt,
            @NotBlank @Size(max = 250) String regulatorNotificationReference) {}

    public record EndRequest(
            @NotBlank @Size(max = 2000) String reason,
            @NotNull Instant effectiveTo) {}

    public record CancelRequest(@NotBlank @Size(max = 2000) String reason) {}
}
