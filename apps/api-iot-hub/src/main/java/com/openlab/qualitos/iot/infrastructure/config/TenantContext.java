package com.openlab.qualitos.iot.infrastructure.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Extract the tenantId from the JWT — NEVER from the request body or query
 * params (CLAUDE.md §18.2 rule 2). All controllers must call
 * {@link #requireTenantId()} as the first line of any handler.
 */
public final class TenantContext {

  /** Claim name carrying the tenant UUID. */
  public static final String TENANT_CLAIM = "tenant_id";

  private TenantContext() {}

  public static UUID requireTenantId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new IllegalStateException("No authenticated principal");
    }
    if (!(auth.getPrincipal() instanceof Jwt jwt)) {
      throw new IllegalStateException("Authentication is not a JWT");
    }
    Object claim = jwt.getClaim(TENANT_CLAIM);
    if (claim == null) {
      throw new IllegalStateException("JWT missing tenant_id claim");
    }
    try {
      return UUID.fromString(claim.toString());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Invalid tenant_id claim", e);
    }
  }
}
