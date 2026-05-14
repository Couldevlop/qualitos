package com.openlab.qualitos.core.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO pour les réponses et requêtes liées aux tenants.
 * Utilise des records Java 21 pour l'immuabilité.
 */
public sealed interface TenantDto permits TenantDto.Response, TenantDto.CreateRequest, TenantDto.UpdateRequest {

    record Response(
        UUID id,
        String slug,
        String name,
        Tenant.Plan plan,
        boolean active,
        Instant createdAt,
        Instant updatedAt
    ) implements TenantDto {

        public static Response from(Tenant tenant) {
            return new Response(
                tenant.getId(),
                tenant.getSlug(),
                tenant.getName(),
                tenant.getPlan(),
                tenant.isActive(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
            );
        }
    }

    record CreateRequest(
        @NotBlank
        @Size(min = 3, max = 63)
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$",
                 message = "Slug must be lowercase alphanumeric with hyphens, 3-63 chars")
        String slug,

        @NotBlank
        @Size(max = 255)
        String name,

        Tenant.Plan plan
    ) implements TenantDto {}

    record UpdateRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        Tenant.Plan plan,

        Boolean active
    ) implements TenantDto {}
}
