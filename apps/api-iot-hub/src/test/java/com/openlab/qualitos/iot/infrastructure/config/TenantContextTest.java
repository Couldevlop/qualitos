package com.openlab.qualitos.iot.infrastructure.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireTenantId_noAuthentication_throws() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated principal");
    }

    @Test
    void requireTenantId_principalIsNotJwt_throws() {
        // Authenticated, but principal is a plain string (not a Jwt)
        var auth = new UsernamePasswordAuthenticationToken(
                "alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a JWT");
    }

    @Test
    void requireTenantId_jwtMissingTenantClaim_throws() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "alice")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        var auth = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing tenant_id claim");
    }

    @Test
    void requireTenantId_jwtWithInvalidTenantClaim_throws() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim(TenantContext.TENANT_CLAIM, "not-a-uuid")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        var auth = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid tenant_id claim");
    }

    @Test
    void requireTenantId_validJwt_returnsTenant() {
        UUID tenant = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim(TenantContext.TENANT_CLAIM, tenant.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        var auth = new UsernamePasswordAuthenticationToken(jwt, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(TenantContext.requireTenantId()).isEqualTo(tenant);
    }

    @Test
    void requireTenantId_anonymousWithNullPrincipal_throws() {
        // Covers the auth != null && auth.getPrincipal() == null branch of
        // the short-circuited `auth == null || auth.getPrincipal() == null`.
        var anon = new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            @Override
            public Object getPrincipal() {
                return null;
            }
        };
        SecurityContextHolder.getContext().setAuthentication(anon);
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated principal");
    }
}
