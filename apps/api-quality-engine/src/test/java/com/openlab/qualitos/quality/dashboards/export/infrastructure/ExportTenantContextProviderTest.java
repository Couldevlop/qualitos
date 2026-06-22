package com.openlab.qualitos.quality.dashboards.export.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportTenantContextProviderTest {

    final ExportTenantContextProvider provider = new ExportTenantContextProvider();

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireTenantId_fromContext() {
        UUID t = UUID.randomUUID();
        TenantContext.setTenantId(t.toString());
        assertThat(provider.requireTenantId()).isEqualTo(t);
    }

    @Test
    void requireTenantId_noTenant_throws() {
        assertThatThrownBy(provider::requireTenantId)
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void requireUserId_fromJwtSub() {
        UUID sub = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(sub.toString()).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .claim("scope", "read").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        assertThat(provider.requireUserId()).isEqualTo(sub);
    }

    @Test
    void requireUserId_noJwt_throws() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("u", "p"));
        assertThatThrownBy(provider::requireUserId)
                .isInstanceOf(MissingTenantContextException.class);
    }
}
