package com.openlab.qualitos.quality.standards.normdoc.dossier.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTO d'entrée du contrôleur (validation Jakarta, OWASP ASVS V5). Ne porte JAMAIS
 * de {@code tenant_id} ni d'identifiant d'acteur : ces données proviennent du JWT
 * (§18.2 #2/#5).
 */
public final class DossierWebDto {

    private DossierWebDto() {}

    public record TenantProfileBody(
            @NotBlank @Size(max = 500) String organizationName,
            @Size(max = 200) String industry,
            @Size(max = 100) String size,
            @Size(max = 16) String language,
            @Size(max = 50) List<@Size(max = 200) String> knownProcesses) {}

    /** Démarrage d'un dossier. {@code documentKeys} vide/absent = plan complet. */
    public record StartBody(
            @NotNull UUID standardId,
            @NotNull @Valid TenantProfileBody tenantProfile,
            @Size(max = 50) List<@Size(max = 100) String> documentKeys) {}

    /** Finalisation : signature humaine globale (obligatoire). */
    public record FinalizeBody(
            @NotBlank @Size(max = 100_000) String signature,
            @Size(max = 4000) String notes) {}
}
