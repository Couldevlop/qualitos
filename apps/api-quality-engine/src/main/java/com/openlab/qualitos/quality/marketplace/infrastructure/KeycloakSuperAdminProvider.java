package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.marketplace.application.SuperAdminProvider;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStateException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KeycloakSuperAdminProvider implements SuperAdminProvider {

    @Override
    public UUID requireSuperAdminId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken token)) {
            throw new MissingTenantContextException();
        }
        boolean isSuperAdmin = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equalsIgnoreCase("ROLE_SUPER_ADMIN") ||
                               a.equalsIgnoreCase("SUPER_ADMIN"));
        if (!isSuperAdmin) {
            throw new MarketplacePackStateException(
                    "marketplace operation requires SUPER_ADMIN role");
        }
        String sub = token.getToken().getSubject();
        if (sub == null) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(sub);
    }
}
