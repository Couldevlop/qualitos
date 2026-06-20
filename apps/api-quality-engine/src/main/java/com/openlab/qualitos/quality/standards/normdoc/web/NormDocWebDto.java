package com.openlab.qualitos.quality.standards.normdoc.web;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTO web (entrée) du module de génération de documents normatifs (§8.8).
 * Aucun {@code tenant_id} ni identifiant d'approbateur ici : ils proviennent du
 * JWT (CLAUDE.md §18.2 #2/#5). Validation Jakarta (OWASP A03).
 */
public final class NormDocWebDto {

    private NormDocWebDto() {}

    public record TenantProfileBody(
            @NotBlank @Size(max = 250) String organizationName,
            @NotBlank @Size(max = 120) String industry,
            @NotBlank @Size(max = 120) String size,
            @Size(max = 10) String language,
            @Size(max = 50) List<@NotBlank @Size(max = 200) String> knownProcesses) {}

    public record SectionSpecBody(
            @NotBlank @Size(max = 120) String key,
            @NotBlank @Size(max = 300) String title,
            @Size(max = 50) List<@NotBlank @Size(max = 50) String> clauses,
            @Size(max = 2000) String guidance) {}

    public record GenerateBody(
            @NotNull UUID standardId,
            @NotNull NormDocKind kind,
            @NotNull @Valid TenantProfileBody tenantProfile,
            @NotEmpty @Size(max = 40) List<@Valid SectionSpecBody> sections) {}

    public record SectionBody(
            @NotBlank @Size(max = 120) String key,
            @NotBlank @Size(max = 300) String title,
            @Size(max = 50) List<@NotBlank @Size(max = 50) String> clauses,
            @NotNull @Size(max = 20000) String bodyMarkdown) {}

    public record EditBody(
            @NotBlank @Size(max = 500) String title,
            @NotEmpty @Size(max = 40) List<@Valid SectionBody> sections) {}

    public record ApproveBody(
            @NotBlank @Size(max = 512) String signature,
            @Size(max = 4000) String notes) {}

    public record RejectBody(
            @NotBlank @Size(max = 2000) String reason) {}
}
