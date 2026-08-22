package com.openlab.qualitos.quality.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Lit dans le jeton la preuve qu'un second facteur a été présenté.
 *
 * <p>Le rôle ne suffit pas : un directeur qualité entré par mot de passe seul
 * porte le même rôle qu'un directeur qualité entré par mot de passe ET code
 * TOTP. Seul le jeton distingue les deux, par ses revendications {@code acr}
 * (niveau d'authentification atteint) et {@code amr} (méthodes employées).
 *
 * <p>Le contrôle est **fail-closed** : en l'absence de revendication lisible, il
 * répond non. Une preuve qu'on ne trouve pas n'est pas une preuve qu'on suppose.
 */
public final class StepUpAuthentication {

    static final String ACR_CLAIM = "acr";
    static final String AMR_CLAIM = "amr";

    private StepUpAuthentication() {}

    /**
     * @return vrai si le jeton courant porte la trace d'un second facteur — ou si
     *         l'exigence est désactivée par configuration.
     */
    public static boolean satisfied(StepUpProperties properties) {
        if (!properties.isEnforced()) return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken token)) {
            // Pas de jeton : aucune revendication à lire, donc aucune preuve.
            return false;
        }
        Jwt jwt = token.getToken();
        return acrAccepted(jwt, properties) || amrAccepted(jwt, properties);
    }

    private static boolean acrAccepted(Jwt jwt, StepUpProperties properties) {
        Object raw = jwt.getClaim(ACR_CLAIM);
        return raw != null && contains(properties.getAcceptedAcr(), String.valueOf(raw));
    }

    /**
     * {@code amr} est un tableau au sens de la RFC 8176, mais certains émetteurs
     * sérialisent une valeur unique en chaîne. Les deux formes sont lues : faire
     * dépendre un contrôle de sécurité de ce choix de sérialisation serait absurde.
     */
    private static boolean amrAccepted(Jwt jwt, StepUpProperties properties) {
        Object raw = jwt.getClaim(AMR_CLAIM);
        if (raw == null) return false;
        List<?> methods = raw instanceof List<?> list ? list : List.of(raw);
        return methods.stream()
                .filter(Objects::nonNull)
                .anyMatch(method -> contains(properties.getAcceptedAmr(), String.valueOf(method)));
    }

    /** Comparaison insensible à la casse et aux espaces : un realm se configure à la main. */
    private static boolean contains(List<String> accepted, String value) {
        String needle = value.trim().toLowerCase(Locale.ROOT);
        return accepted != null && accepted.stream()
                .filter(Objects::nonNull)
                .anyMatch(candidate -> candidate.trim().toLowerCase(Locale.ROOT).equals(needle));
    }
}
