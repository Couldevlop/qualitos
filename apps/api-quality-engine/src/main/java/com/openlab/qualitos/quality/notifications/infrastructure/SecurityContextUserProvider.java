package com.openlab.qualitos.quality.notifications.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.notifications.application.UserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Résout l'utilisateur courant via le {@code SecurityContext} (sub du JWT). Sur les
 * endpoints sécurisés, le principal est toujours présent ; en son absence on refuse
 * l'accès (OWASP A01/A07).
 */
@Component
public class SecurityContextUserProvider implements UserProvider {

    @Override
    public String requireUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !StringUtils.hasText(auth.getName())) {
            throw new MissingTenantContextException();
        }
        return auth.getName();
    }
}
