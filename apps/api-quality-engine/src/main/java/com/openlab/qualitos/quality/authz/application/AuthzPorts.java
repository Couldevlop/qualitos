package com.openlab.qualitos.quality.authz.application;

import com.openlab.qualitos.quality.authz.domain.TenantRole;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Ce que l'administration des droits lit et écrit, et de qui elle tient le contexte. */
public final class AuthzPorts {

    private AuthzPorts() {}

    /** Les rôles réglés ou créés par chaque client. */
    public interface Roles {

        List<TenantRole> findByTenant(UUID tenantId);

        void save(UUID tenantId, TenantRole role, UUID actor);

        /** Supprime le rôle ET ses attributions : un rôle disparu n'accorde plus rien à personne. */
        void delete(UUID tenantId, String code);
    }

    /** Les rôles attribués dans l'application, en plus de ceux du jeton. */
    public interface Members {

        Set<String> rolesOf(UUID tenantId, UUID userId);

        Map<UUID, Set<String>> all(UUID tenantId);

        /** Remplace l'ensemble des rôles attribués à un membre. */
        void replace(UUID tenantId, UUID userId, Set<String> roleCodes, UUID actor);
    }

    /** Le client, l'utilisateur et les rôles de son jeton — jamais lus dans une requête (§18.2). */
    public interface Context {

        UUID requireTenantId();

        Optional<UUID> userId();

        UUID requireActorId();

        /** Les rôles que porte le jeton, en codes de rôles système. */
        Set<String> tokenRoles();
    }

    /** La trace opposable des changements de droits, sans texte libre. */
    public interface Audit {

        void roleSaved(UUID tenantId, TenantRole role, boolean created, UUID actor);

        void roleDeleted(UUID tenantId, String code, UUID actor);

        void memberRolesReplaced(UUID tenantId, UUID userId, Set<String> before, Set<String> after, UUID actor);
    }
}
