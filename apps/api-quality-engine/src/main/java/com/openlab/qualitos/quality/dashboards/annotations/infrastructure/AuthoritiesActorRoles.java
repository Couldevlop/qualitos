package com.openlab.qualitos.quality.dashboards.annotations.infrastructure;

import com.openlab.qualitos.quality.dashboards.annotations.application.ActorRoles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Reads the tenant-admin capability from the JWT-derived authorities
 * (cf. SecurityConfig role mapping {@code ROLE_<UPPER>}). Mirrors the role
 * model in CLAUDE.md §16 — admins may moderate others' annotations.
 */
@Component("dashboardAnnotationActorRoles")
public class AuthoritiesActorRoles implements ActorRoles {

    private static final Set<String> ADMIN_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_ADMIN_TENANT", "ROLE_SUPER_ADMIN");

    @Override
    public boolean isTenantAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (ADMIN_ROLES.contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
