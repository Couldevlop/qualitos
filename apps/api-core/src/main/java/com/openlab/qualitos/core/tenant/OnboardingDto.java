package com.openlab.qualitos.core.tenant;

import com.openlab.qualitos.core.user.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Créer un client en une fois : l'entreprise, ses modules, son premier administrateur (ADR 0079). */
public final class OnboardingDto {

    private OnboardingDto() {}

    public record Request(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(min = 3, max = 63)
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$",
                    message = "Slug must be lowercase alphanumeric with hyphens, 3-63 chars")
            String slug,
            Tenant.Plan plan,
            @Size(max = 40) List<@NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$") String> modules,
            @NotNull @Valid Admin admin) {}

    public record Admin(
            @NotBlank @Email @Size(max = 254) String email,
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName) {}

    /** Ce qu'un module est devenu : ouvert, ou refusé par le moteur avec la raison. */
    public record ModuleOutcome(String code, boolean activated, String message) {}

    /**
     * @param temporaryPassword montré UNE fois au super administrateur ; {@code null}
     *                          quand l'invitation est partie par e-mail
     */
    public record Response(TenantDto.Response tenant, UserDto.Response admin, String temporaryPassword,
                           boolean invitationSent, List<ModuleOutcome> modules) {}
}
