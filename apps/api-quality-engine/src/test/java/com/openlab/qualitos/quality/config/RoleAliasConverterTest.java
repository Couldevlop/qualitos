package com.openlab.qualitos.quality.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le realm nomme le rôle {@code quality_director} ; tout le code qualité écrit
 * {@code hasAnyRole('DIRECTOR_QUALITY')}. Sans alias, un vrai directeur qualité
 * porte {@code ROLE_QUALITY_DIRECTOR}, ne correspond à aucune règle, et se voit
 * refuser l'approbation d'un control plan — sans qu'aucun test ne s'en aperçoive,
 * puisque les bancs Web fabriquent l'autorité qu'ils veulent.
 *
 * <p>L'alias est posé une fois, dans le convertisseur, plutôt que répété dans
 * neuf {@code @PreAuthorize} — et il vaut dans les deux sens, pour qu'un jeton
 * émis par l'une ou l'autre convention ouvre les mêmes portes.
 */
class RoleAliasConverterTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void theRealmRoleOpensTheDoorTheCodeGuards() {
        Collection<GrantedAuthority> authorities = convert("quality_director");

        assertThat(names(authorities))
                .contains("ROLE_QUALITY_DIRECTOR", "ROLE_DIRECTOR_QUALITY");
    }

    @Test
    void theAliasWorksInTheOtherDirectionToo() {
        // Un jeton émis par un realm qui aurait retenu l'autre nom ne doit pas
        // devenir un cas particulier à traiter ailleurs.
        Collection<GrantedAuthority> authorities = convert("director_quality");

        assertThat(names(authorities))
                .contains("ROLE_DIRECTOR_QUALITY", "ROLE_QUALITY_DIRECTOR");
    }

    @Test
    void theOtherRolesAreLeftExactlyAsTheRealmSpellsThem() {
        Collection<GrantedAuthority> authorities =
                convert("super_admin", "admin_tenant", "quality_manager", "user");

        assertThat(names(authorities)).containsExactlyInAnyOrder(
                "ROLE_SUPER_ADMIN", "ROLE_ADMIN_TENANT", "ROLE_QUALITY_MANAGER", "ROLE_USER");
    }

    @Test
    void aTokenWithoutRealmAccessGrantsNothing() {
        Jwt jwt = new Jwt("t", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of("sub", "u"));

        assertThat(config.jwtAuthenticationConverter().convert(jwt).getAuthorities()).isEmpty();
    }

    @Test
    void aRealmAccessWithoutRolesGrantsNothing() {
        Jwt jwt = new Jwt("t", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of("sub", "u", "realm_access", Map.of()));

        assertThat(config.jwtAuthenticationConverter().convert(jwt).getAuthorities()).isEmpty();
    }

    @Test
    void anAliasIsNeverGrantedTwice() {
        // Un realm qui porterait les deux noms ne doit pas produire de doublon :
        // une autorité en double se lit mal dans les journaux de refus.
        Collection<GrantedAuthority> authorities = convert("quality_director", "director_quality");

        assertThat(names(authorities))
                .containsExactlyInAnyOrder("ROLE_QUALITY_DIRECTOR", "ROLE_DIRECTOR_QUALITY");
    }

    private Collection<GrantedAuthority> convert(String... realmRoles) {
        Jwt jwt = new Jwt("t", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "u", "realm_access", Map.of("roles", List.of(realmRoles))));
        return config.jwtAuthenticationConverter().convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private static List<String> names(Collection<GrantedAuthority> authorities) {
        return AuthorityUtils.authorityListToSet(authorities).stream().sorted().toList();
    }
}
