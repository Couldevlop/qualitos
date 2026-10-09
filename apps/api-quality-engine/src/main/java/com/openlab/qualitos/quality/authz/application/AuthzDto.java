package com.openlab.qualitos.quality.authz.application;

import java.util.List;
import java.util.UUID;

/** Les vues et commandes de l'administration des droits. Des codes ; les mots se composent à l'écran. */
public final class AuthzDto {

    private AuthzDto() {}

    /** Une action du catalogue et son module. */
    public record CatalogEntry(String code, String module) {}

    /** Ce que l'utilisateur courant peut faire, pour que l'écran n'affiche que ce qui servira. */
    public record Me(UUID userId, List<String> roles, List<String> permissions) {}

    /**
     * Un rôle du client.
     *
     * @param customized faux pour un rôle système encore aux droits livrés par la plateforme
     */
    public record RoleView(String code, String name, String description, boolean system, boolean customized,
                           List<String> permissions) {}

    public record RoleCommand(String code, String name, String description, List<String> permissions) {}

    /** Les rôles attribués dans l'application à un membre (en plus de ceux de son compte). */
    public record MemberView(UUID userId, List<String> roles) {}
}
