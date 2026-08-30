package com.openlab.qualitos.quality.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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

    /**
     * Nom lisible de l'utilisateur courant, tel que l'annuaire le connaît
     * ({@code name}, à défaut {@code preferred_username}).
     *
     * <p>Lu dans le JETON, jamais dans le body : un « détecté par » que
     * l'appelant pourrait écrire lui-même attribuerait un signalement à
     * n'importe qui, ce qui est précisément ce qu'une piste d'audit doit
     * empêcher (OWASP A01, même invariant que {@link #userId()}).
     *
     * <p>Vide hors contexte authentifié, ou si le jeton ne porte aucun de ces
     * deux claims : mieux vaut une colonne vide qu'un nom reconstitué.
     *
     * @return le nom d'affichage, sans espaces superflus, ou vide.
     */
    public static Optional<String> displayName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        String name = jwt.getClaimAsString("name");
        if (!StringUtils.hasText(name)) {
            name = jwt.getClaimAsString("preferred_username");
        }
        return StringUtils.hasText(name) ? Optional.of(name.trim()) : Optional.empty();
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
