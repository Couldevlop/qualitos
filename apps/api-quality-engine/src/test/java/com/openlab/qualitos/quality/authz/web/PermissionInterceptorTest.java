package com.openlab.qualitos.quality.authz.web;

import com.openlab.qualitos.quality.authz.application.AuthorizationService;
import com.openlab.qualitos.quality.authz.domain.Permission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionInterceptorTest {

    @RequiresPermission(Permission.NC_CLOSE)
    static class Controleur {
        public void close() { /* banc */ }

        @RequiresPermission(Permission.NC_CREATE)
        public void create() { /* banc */ }
    }

    static class Libre {
        public void lire() { /* banc */ }
    }

    final MockHttpServletRequest req = new MockHttpServletRequest();
    final MockHttpServletResponse res = new MockHttpServletResponse();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    static HandlerMethod handler(Object bean, String methode) throws NoSuchMethodException {
        return new HandlerMethod(bean, bean.getClass().getMethod(methode));
    }

    static void connecte(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("u", "p",
                java.util.Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()));
    }

    @SuppressWarnings("unchecked")
    static ObjectProvider<AuthorizationService> fournisseur(AuthorizationService s) {
        ObjectProvider<AuthorizationService> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(s);
        return p;
    }

    @Test
    void leServiceTrancheEtLaMethodePrimeSurLaClasse() throws Exception {
        AuthorizationService service = mock(AuthorizationService.class);
        when(service.has(Permission.NC_CREATE)).thenReturn(true);
        PermissionInterceptor i = new PermissionInterceptor(fournisseur(service));
        connecte("USER");

        assertThat(i.preHandle(req, res, handler(new Controleur(), "create"))).isTrue();
        assertThatThrownBy(() -> i.preHandle(req, res, handler(new Controleur(), "close")))
                .isInstanceOf(AccessDeniedException.class);
        verify(service).has(Permission.NC_CLOSE);
    }

    @Test
    void sansServiceLesDroitsLivresS_appliquentJamaisToutPermis() throws Exception {
        PermissionInterceptor i = new PermissionInterceptor(fournisseur(null));

        connecte("USER");
        assertThat(i.preHandle(req, res, handler(new Controleur(), "create"))).isTrue();
        assertThatThrownBy(() -> i.preHandle(req, res, handler(new Controleur(), "close")))
                .isInstanceOf(AccessDeniedException.class);

        connecte("QUALITY_MANAGER");
        assertThat(i.preHandle(req, res, handler(new Controleur(), "close"))).isTrue();
    }

    @Test
    void sansAnnotationNiAuthentificationIlNeSePrononcePas() throws Exception {
        AuthorizationService service = mock(AuthorizationService.class);
        PermissionInterceptor i = new PermissionInterceptor(fournisseur(service));

        connecte("USER");
        assertThat(i.preHandle(req, res, handler(new Libre(), "lire"))).isTrue();
        assertThat(i.preHandle(req, res, new Object())).isTrue();

        SecurityContextHolder.clearContext();
        assertThat(i.preHandle(req, res, handler(new Controleur(), "close"))).isTrue();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("k", "anon",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThat(i.preHandle(req, res, handler(new Controleur(), "close"))).isTrue();
        verify(service, never()).has(Permission.NC_CLOSE);
    }
}
