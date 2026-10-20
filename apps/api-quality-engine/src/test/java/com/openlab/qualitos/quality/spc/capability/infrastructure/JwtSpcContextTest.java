package com.openlab.qualitos.quality.spc.capability.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Le tenant et l'acteur sortent du jeton, et seulement de lui. */
class JwtSpcContextTest {

    final JwtSpcContext context = new JwtSpcContext();

    @AfterEach
    void nettoyer() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void sansTenantDansLeJetonRienNePasse() {
        assertThatThrownBy(context::requireTenantId).isInstanceOf(MissingTenantContextException.class);
        assertThatThrownBy(context::requireActorId).isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void leTenantEtLActeurViennentDuJeton() {
        UUID tenant = UUID.randomUUID();
        UUID sub = UUID.randomUUID();
        TenantContext.setTenantId(tenant.toString());
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub.toString())
                .issuedAt(Instant.EPOCH).expiresAt(Instant.EPOCH.plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));

        assertThat(context.requireTenantId()).isEqualTo(tenant);
        assertThat(context.requireActorId()).isEqualTo(sub);
    }
}
