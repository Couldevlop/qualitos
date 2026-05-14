package com.openlab.qualitos.quality.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extrait le claim {@code tenant_id} du JWT Bearer et le stocke dans {@link TenantContext}.
 * Le contexte est nettoyé dans finally pour éviter toute fuite entre threads du pool Tomcat.
 */
public class TenantJwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantJwtFilter.class);

    static final String BEARER_PREFIX = "Bearer ";
    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String TENANT_ID_CLAIM = "tenant_id";

    private final JwtDecoder jwtDecoder;

    public TenantJwtFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            extractAndStoreTenantId(request);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void extractAndStoreTenantId(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith(BEARER_PREFIX)) {
            return;
        }

        String token = bearerToken.substring(BEARER_PREFIX.length());

        try {
            Jwt jwt = jwtDecoder.decode(token);
            String tenantId = jwt.getClaimAsString(TENANT_ID_CLAIM);

            if (StringUtils.hasText(tenantId)) {
                TenantContext.setTenantId(tenantId);
                log.debug("Tenant context set for request {} {}", request.getMethod(), request.getRequestURI());
            } else {
                log.warn("JWT decoded but claim '{}' is absent or empty — request {} {}",
                        TENANT_ID_CLAIM, request.getMethod(), request.getRequestURI());
            }
        } catch (JwtException ex) {
            log.debug("JWT decoding skipped (will be rejected by Spring Security): {}", ex.getMessage());
        }
    }
}
