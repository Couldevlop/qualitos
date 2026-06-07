package com.openlab.qualitos.quality.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * Résout l'utilisateur courant (le {@code sub} du JWT) depuis le
 * {@link SecurityContextHolder}.
 *
 * <p>Invariant de sécurité (OWASP A01) : l'attribution d'acteur d'une action
 * (qui a créé une NC, activé un pack…) provient TOUJOURS de l'identité
 * authentifiée, jamais du body de la requête, qui est falsifiable. Sur les
 * endpoints sécurisés, le principal est toujours présent ; en son absence on
 * refuse l'accès.</p>
 *
 * <p>Le {@code sub} Keycloak est un UUID ; {@link #requireUserId()} le renvoie
 * typé. {@link #userId()} renvoie un {@link Optional} vide si le contexte est
 * absent ou si le sub n'est pas un UUID exploitable.</p>
 */
public final class CurrentUser {

    private CurrentUser() {}

    /** @return le sub du JWT typé en UUID, ou lève {@link MissingTenantContextException}. */
    public static UUID requireUserId() {
        return userId().orElseThrow(MissingTenantContextException::new);
    }

    /** @return le sub du JWT typé en UUID si présent et bien formé, sinon vide. */
    public static Optional<UUID> userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !StringUtils.hasText(auth.getName())) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(auth.getName()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
