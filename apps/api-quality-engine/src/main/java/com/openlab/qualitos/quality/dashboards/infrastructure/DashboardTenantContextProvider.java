package com.openlab.qualitos.quality.dashboards.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.dashboards.application.TenantProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tenant + user from JWT only.
 * `tid` claim → tenantId; `sub` claim → userId.
 */
@Component("dashboardsTenantContextProvider")
public class DashboardTenantContextProvider implements TenantProvider {

    @Override
    public UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    @Override
    public UUID requireUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken token)) {
            throw new MissingTenantContextException();
        }
        Jwt jwt = token.getToken();
        String sub = jwt.getSubject();
        if (sub == null) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(sub);
    }
}
