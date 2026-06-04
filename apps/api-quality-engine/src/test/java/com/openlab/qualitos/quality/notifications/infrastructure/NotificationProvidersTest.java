package com.openlab.qualitos.quality.notifications.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationProvidersTest {

    private final TenantContextProvider tenantProvider = new TenantContextProvider();
    private final SecurityContextUserProvider userProvider = new SecurityContextUserProvider();

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantProvider_returnsTenantFromContext() {
        UUID t = UUID.randomUUID();
        TenantContext.setTenantId(t.toString());
        assertThat(tenantProvider.requireTenantId()).isEqualTo(t);
    }

    @Test
    void tenantProvider_throwsWithoutContext() {
        assertThatThrownBy(tenantProvider::requireTenantId)
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void userProvider_returnsSubjectName() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("kc-sub-123");
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThat(userProvider.requireUserId()).isEqualTo("kc-sub-123");
    }

    @Test
    void userProvider_throwsWhenNoAuthentication() {
        assertThatThrownBy(userProvider::requireUserId)
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void userProvider_throwsWhenNotAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThatThrownBy(userProvider::requireUserId)
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void userProvider_throwsWhenNameBlank() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("  ");
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThatThrownBy(userProvider::requireUserId)
                .isInstanceOf(MissingTenantContextException.class);
    }
}
