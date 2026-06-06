package com.openlab.qualitos.quality.tenantmodules.web;

import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

/**
 * Corps de requête des mutations d'activation modules.
 *
 * <p>H2 (OWASP A01) : aucun champ {@code actor} n'est accepté dans le corps —
 * l'acteur est dérivé du JWT côté service ({@code ActorProvider}). Tout
 * {@code actor} envoyé par le client est silencieusement ignoré
 * (propriété inconnue tolérée par la désérialisation par défaut).</p>
 */
public final class ModuleActivationWebDto {

    private ModuleActivationWebDto() {}

    public record StartTrialRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,49}$") String moduleCode,
            @NotNull Instant trialEndsAt) {}

    public record ActivateRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,49}$") String moduleCode,
            Instant expiresAt) {}

    public record ConvertTrialRequest(Instant expiresAt) {}
    public record SuspendRequest() {}
    public record ResumeRequest() {}
    public record DisableRequest() {}
    public record ExpireRequest() {}
    public record ChangeTierRequest(@NotNull BillingTier newTier) {}
    public record ConfigureRequest(String configurationJson) {}
}
