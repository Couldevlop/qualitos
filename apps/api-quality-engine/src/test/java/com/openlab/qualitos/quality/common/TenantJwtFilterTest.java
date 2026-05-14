package com.openlab.qualitos.quality.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantJwtFilterTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private TenantJwtFilter filter;

    @AfterEach
    void cleanupContext() {
        TenantContext.clear();
    }

    @Test
    void setsContextWhenValidJwtWithTenantId() throws ServletException, IOException {
        String tenantId = "tenant-abc-123";
        String rawToken = "valid.jwt.token";

        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn(TenantJwtFilter.BEARER_PREFIX + rawToken);
        given(jwtDecoder.decode(rawToken)).willReturn(buildJwt(tenantId));

        doAnswer(inv -> {
            assertThat(TenantContext.getTenantId()).isEqualTo(tenantId);
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void clearsTenantContextOnException() throws ServletException, IOException {
        String rawToken = "valid.jwt.token";

        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn(TenantJwtFilter.BEARER_PREFIX + rawToken);
        given(jwtDecoder.decode(rawToken)).willReturn(buildJwt("tenant-err"));
        doThrow(new ServletException("boom")).when(filterChain).doFilter(request, response);

        try {
            filter.doFilterInternal(request, response, filterChain);
        } catch (ServletException ignored) {
            // attendu
        }

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void doesNotSetContextWhenNoAuthorizationHeader() throws ServletException, IOException {
        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER)).willReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtDecoder, never()).decode(anyString());
        verify(filterChain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void doesNotSetContextWhenNotBearerToken() throws ServletException, IOException {
        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtDecoder, never()).decode(anyString());
        verify(filterChain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void doesNotSetContextWhenJwtLacksTenantIdClaim() throws ServletException, IOException {
        String rawToken = "jwt.without.tenant";

        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn(TenantJwtFilter.BEARER_PREFIX + rawToken);
        given(jwtDecoder.decode(rawToken)).willReturn(buildJwtWithoutTenantId());

        doAnswer(inv -> {
            assertThat(TenantContext.getTenantId()).isNull();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);
    }

    @Test
    void doesNotSetContextWhenJwtIsInvalid() throws ServletException, IOException {
        String rawToken = "invalid.token";

        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn(TenantJwtFilter.BEARER_PREFIX + rawToken);
        given(jwtDecoder.decode(rawToken)).willThrow(new JwtException("Bad token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void doesNotSetContextWhenTenantIdClaimIsBlank() throws ServletException, IOException {
        String rawToken = "jwt.blank.tenant";

        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn(TenantJwtFilter.BEARER_PREFIX + rawToken);
        given(jwtDecoder.decode(rawToken)).willReturn(buildJwtWithBlankTenantId());

        doAnswer(inv -> {
            assertThat(TenantContext.getTenantId()).isNull();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);
    }

    // -------------------------------------------------------------------------

    private Jwt buildJwt(String tenantId) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("user-sub")
                .claims(c -> c.put(TenantJwtFilter.TENANT_ID_CLAIM, tenantId))
                .build();
    }

    private Jwt buildJwtWithoutTenantId() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("user-sub")
                .claims(c -> {})
                .build();
    }

    private Jwt buildJwtWithBlankTenantId() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("user-sub")
                .claims(c -> c.put(TenantJwtFilter.TENANT_ID_CLAIM, "   "))
                .build();
    }
}
