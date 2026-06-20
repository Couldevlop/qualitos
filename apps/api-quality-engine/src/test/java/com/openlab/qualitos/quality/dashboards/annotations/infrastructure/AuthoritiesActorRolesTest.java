package com.openlab.qualitos.quality.dashboards.annotations.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthoritiesActorRolesTest {

    final AuthoritiesActorRoles roles = new AuthoritiesActorRoles();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthentication_isNotAdmin() {
        SecurityContextHolder.clearContext();
        assertThat(roles.isTenantAdmin()).isFalse();
    }

    @Test
    void unauthenticated_isNotAdmin() {
        var anon = new AnonymousAuthenticationToken("key", "anon",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        anon.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(anon);
        assertThat(roles.isTenantAdmin()).isFalse();
    }

    @Test
    void plainUser_isNotAdmin() {
        authenticate("ROLE_USER", "ROLE_QUALITY_MANAGER");
        assertThat(roles.isTenantAdmin()).isFalse();
    }

    @Test
    void adminTenant_isAdmin() {
        authenticate("ROLE_USER", "ROLE_ADMIN_TENANT");
        assertThat(roles.isTenantAdmin()).isTrue();
    }

    @Test
    void superAdmin_isAdmin() {
        authenticate("ROLE_SUPER_ADMIN");
        assertThat(roles.isTenantAdmin()).isTrue();
    }

    @Test
    void legacyAdmin_isAdmin() {
        authenticate("ROLE_ADMIN");
        assertThat(roles.isTenantAdmin()).isTrue();
    }

    private static void authenticate(String... authorities) {
        var auth = new UsernamePasswordAuthenticationToken(
                "user", "n/a",
                java.util.Arrays.stream(authorities)
                        .map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
