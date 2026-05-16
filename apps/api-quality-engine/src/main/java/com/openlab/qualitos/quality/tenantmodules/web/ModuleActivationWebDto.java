package com.openlab.qualitos.quality.tenantmodules.web;

import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.UUID;

public final class ModuleActivationWebDto {

    private ModuleActivationWebDto() {}

    public record StartTrialRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,49}$") String moduleCode,
            @NotNull Instant trialEndsAt,
            @NotNull UUID actor) {}

    public record ActivateRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,49}$") String moduleCode,
            Instant expiresAt,
            @NotNull UUID actor) {}

    public record ConvertTrialRequest(Instant expiresAt, @NotNull UUID actor) {}
    public record SuspendRequest(@NotNull UUID actor) {}
    public record ResumeRequest(@NotNull UUID actor) {}
    public record DisableRequest(@NotNull UUID actor) {}
    public record ExpireRequest(@NotNull UUID actor) {}
    public record ChangeTierRequest(@NotNull BillingTier newTier, @NotNull UUID actor) {}
    public record ConfigureRequest(String configurationJson, @NotNull UUID actor) {}
}
