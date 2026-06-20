package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Providers tenant/acteur depuis le JWT (§8.4 onglet 7, OWASP A01). */
class MockAuditProvidersTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantProvider_readsTenantContext() {
        UUID tenant = UUID.randomUUID();
        TenantContext.setTenantId(tenant.toString());
        assertThat(new MockAuditTenantContextProvider().requireTenantId()).isEqualTo(tenant);
    }

    @Test
    void tenantProvider_missingTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> new MockAuditTenantContextProvider().requireTenantId())
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void actorProvider_readsJwtSubject() {
        UUID user = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.toString(), "n/a", List.of()));
        assertThat(new MockAuditCurrentUserActorProvider().requireActorId()).isEqualTo(user);
    }

    @Test
    void actorProvider_noAuth_throws() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> new MockAuditCurrentUserActorProvider().requireActorId())
                .isInstanceOf(MissingTenantContextException.class);
    }
}
