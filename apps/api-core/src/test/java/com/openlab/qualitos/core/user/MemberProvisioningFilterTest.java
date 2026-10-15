package com.openlab.qualitos.core.user;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** La provision ne fait jamais échouer la requête, et ne s'applique qu'à un jeton authentifié. */
class MemberProvisioningFilterTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MemberProvisioning> provider(MemberProvisioning p) {
        ObjectProvider<MemberProvisioning> o = mock(ObjectProvider.class);
        given(o.getIfAvailable()).willReturn(p);
        return o;
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue("t").header("alg", "none").subject("s").build();
    }

    @Test
    void unePanneDeProvisionLaissePasserLaRequete() throws Exception {
        MemberProvisioning p = mock(MemberProvisioning.class);
        given(p.provision(any())).willThrow(new IllegalStateException("base indisponible"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt(), List.of()));
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        new MemberProvisioningFilter(provider(p)).doFilter(req, res, chain);

        verify(p).provision(any());
        verify(chain).doFilter(req, res);
    }

    @Test
    void sansJetonOuSansServiceRienNEstTente() throws Exception {
        MemberProvisioning p = mock(MemberProvisioning.class);
        FilterChain chain = mock(FilterChain.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", "p", List.of()));
        new MemberProvisioningFilter(provider(p)).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
        verifyNoInteractions(p);

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt(), List.of()));
        new MemberProvisioningFilter(provider(null)).doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
        verify(chain, org.mockito.Mockito.times(2)).doFilter(any(), any());
    }
}
