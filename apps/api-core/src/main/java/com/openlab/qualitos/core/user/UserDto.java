package com.openlab.qualitos.core.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTOs pour les utilisateurs applicatifs.
 * Le tenantId n'est JAMAIS accepté depuis le body — il est extrait du JWT.
 */
public sealed interface UserDto permits UserDto.Response, UserDto.CreateRequest, UserDto.UpdateRequest,
        UserDto.InviteRequest, UserDto.InviteResponse {

    record Response(
        UUID id,
        UUID tenantId,
        String keycloakId,
        String email,
        Set<String> roles,
        boolean active,
        Instant createdAt,
        Instant updatedAt
    ) implements UserDto {

        public static Response from(AppUser user) {
            return new Response(
                user.getId(),
                user.getTenantId(),
                user.getKeycloakId(),
                user.getEmail(),
                user.getRoles(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
            );
        }
    }

    /**
     * Le tenantId est intentionnellement absent : il est injecté par le service
     * depuis TenantContext (claim JWT), jamais depuis ce body.
     */
    record CreateRequest(
        @NotBlank
        String keycloakId,

        @NotBlank
        @Email
        String email,

        @NotEmpty
        Set<String> roles
    ) implements UserDto {}

    record UpdateRequest(
        @NotEmpty
        Set<String> roles,

        Boolean active
    ) implements UserDto {}

    /**
     * Inviter un membre : son compte de connexion est créé dans le client du
     * jeton, avec ses rôles. Ni client ni identifiant de compte dans le corps.
     */
    record InviteRequest(
        @NotBlank @Email @Size(max = 254)
        String email,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @NotEmpty @Size(max = 6)
        Set<@NotBlank @Size(max = 32) String> roles
    ) implements UserDto {}

    /**
     * Le membre invité, et comment il entrera.
     *
     * @param temporaryPassword montré UNE fois à l'administrateur ; {@code null}
     *                          quand l'invitation est partie par e-mail
     */
    record InviteResponse(
        Response user,
        String temporaryPassword,
        boolean invitationSent
    ) implements UserDto {}
}
