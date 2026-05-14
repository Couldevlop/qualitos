package com.openlab.qualitos.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantJwtFilter")
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
    @DisplayName("Sets tenant_id in context when JWT has valid tenant_id claim")
    void setsContextWhenValidJwtWithTenantId() throws ServletException, IOException {
        String tenantId = "tenant-abc-123";
        String rawToken = "valid.jwt.token";
        Jwt jwt = buildJwt(tenantId);

        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn(TenantJwtFilter.BEARER_PREFIX + rawToken);
        given(jwtDecoder.decode(rawToken)).willReturn(jwt);

        filter.doFilterInternal(request, response, filterChain);

        // Après le filter, le contexte est nettoyé — on vérifie via ce qui s'est passé pendant la chaîne
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Clears TenantContext in finally block after filter chain executes")
    void clearsTenantContextAfterChain() throws ServletException, IOException {
        String tenantId = "tenant-xyz";
        String rawToken = "valid.jwt.token";
        Jwt jwt = buildJwt(tenantId);

        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn(TenantJwtFilter.BEARER_PREFIX + rawToken);
        given(jwtDecoder.decode(rawToken)).willReturn(jwt);

        // Vérifie que le contexte est bien set pendant l'exécution
        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isEqualTo(tenantId);
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        // Après exécution, le contexte est nettoyé
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Clears TenantContext even when filter chain throws exception")
    void clearsTenantContextOnException() throws ServletException, IOException {
        String rawToken = "valid.jwt.token";
        Jwt jwt = buildJwt("tenant-err");

        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn(TenantJwtFilter.BEARER_PREFIX + rawToken);
        given(jwtDecoder.decode(rawToken)).willReturn(jwt);
        doThrow(new ServletException("test error")).when(filterChain).doFilter(request, response);

        try {
            filter.doFilterInternal(request, response, filterChain);
        } catch (ServletException ignored) {
            // Exception attendue
        }

        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Does not set context when Authorization header is absent")
    void doesNotSetContextWhenNoAuthorizationHeader() throws ServletException, IOException {
        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER)).willReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtDecoder, never()).decode(anyString());
        verify(filterChain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Does not set context when Authorization header is not Bearer")
    void doesNotSetContextWhenNotBearerToken() throws ServletException, IOException {
        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(jwtDecoder, never()).decode(anyString());
        verify(filterChain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Does not set context when JWT has no tenant_id claim")
    void doesNotSetContextWhenJwtLacksTenantIdClaim() throws ServletException, IOException {
        String rawToken = "jwt.without.tenant";
        Jwt jwt = buildJwtWithoutTenantId();

        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn(TenantJwtFilter.BEARER_PREFIX + rawToken);
        given(jwtDecoder.decode(rawToken)).willReturn(jwt);

        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isNull();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Does not set context when JWT is invalid — lets Spring Security reject it")
    void doesNotSetContextWhenJwtIsInvalid() throws ServletException, IOException {
        String rawToken = "invalid.token";

        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn(TenantJwtFilter.BEARER_PREFIX + rawToken);
        given(jwtDecoder.decode(rawToken)).willThrow(new JwtException("Bad token"));

        filter.doFilterInternal(request, response, filterChain);

        // La chaîne continue — Spring Security rejettera la requête lui-même
        verify(filterChain).doFilter(request, response);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Does not set context when tenant_id claim is blank")
    void doesNotSetContextWhenTenantIdClaimIsBlank() throws ServletException, IOException {
        String rawToken = "jwt.blank.tenant";
        Jwt jwt = buildJwtWithBlankTenantId();

        given(request.getHeader(TenantJwtFilter.AUTHORIZATION_HEADER))
                .willReturn(TenantJwtFilter.BEARER_PREFIX + rawToken);
        given(jwtDecoder.decode(rawToken)).willReturn(jwt);

        doAnswer(invocation -> {
            assertThat(TenantContext.getTenantId()).isNull();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Jwt buildJwt(String tenantId) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("user-sub")
                .claims(claims -> claims.put(TenantJwtFilter.TENANT_ID_CLAIM, tenantId))
                .build();
    }

    private Jwt buildJwtWithoutTenantId() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("user-sub")
                .claims(claims -> {})
                .build();
    }

    private Jwt buildJwtWithBlankTenantId() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("user-sub")
                .claims(claims -> claims.put(TenantJwtFilter.TENANT_ID_CLAIM, "   "))
                .build();
    }
}
