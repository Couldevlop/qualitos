package com.openlab.qualitos.quality.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le nom affiché comme « détecté par » vient du JETON, jamais du corps de la
 * requête. Un appelant capable d'écrire ce nom lui-même attribuerait un
 * signalement à n'importe qui — exactement ce qu'une piste d'audit doit
 * empêcher (OWASP A01).
 *
 * <p>Et quand le jeton ne dit rien, on ne devine rien : mieux vaut une colonne
 * vide qu'un nom reconstitué qui aurait l'air d'une donnée.
 */
class CurrentUserDisplayNameTest {

    private static final UUID SUB = UUID.randomUUID();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateWith(Map<String, Object> claims) {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "none"), claims);
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of(), SUB.toString()));
    }

    @Test
    void prefersTheDirectoryNameWhenTheTokenCarriesOne() {
        authenticateWith(Map.of("sub", SUB.toString(),
                "name", "Amina Dridi",
                "preferred_username", "adridi"));

        assertThat(CurrentUser.displayName()).contains("Amina Dridi");
    }

    @Test
    void fallsBackOnTheLoginWhenNoFullNameIsPublished() {
        // Un annuaire qui ne publie pas `name` n'est pas une raison d'afficher
        // un UUID : le login reste un identifiant qu'un collègue reconnaît.
        authenticateWith(Map.of("sub", SUB.toString(), "preferred_username", "adridi"));

        assertThat(CurrentUser.displayName()).contains("adridi");
    }

    @Test
    void trimsTheSurroundingSpacesRatherThanStoringThem() {
        authenticateWith(Map.of("sub", SUB.toString(), "name", "  Amina Dridi  "));

        assertThat(CurrentUser.displayName()).contains("Amina Dridi");
    }

    @Test
    void staysEmptyWhenTheTokenPublishesNeitherClaim() {
        authenticateWith(Map.of("sub", SUB.toString()));

        assertThat(CurrentUser.displayName()).isEmpty();
    }

    @Test
    void staysEmptyOnABlankName() {
        authenticateWith(Map.of("sub", SUB.toString(), "name", "   "));

        assertThat(CurrentUser.displayName()).isEmpty();
    }

    @Test
    void staysEmptyOutsideAnyAuthenticatedContext() {
        assertThat(CurrentUser.displayName()).isEmpty();
    }

    @Test
    void staysEmptyWhenThePrincipalIsNotAJwt() {
        // Bancs de service, tâches de fond : il n'y a pas de jeton à lire, et
        // rien à en déduire.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(SUB.toString(), "n/a", List.of()));

        assertThat(CurrentUser.displayName()).isEmpty();
    }
}
