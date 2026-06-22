package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.marketplace.application.CurrentActorProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implémentation du port {@link CurrentActorProvider} : dérive l'acteur du sub du
 * JWT via {@link CurrentUser} (OWASP A01 — jamais du body). Lève
 * {@code MissingTenantContextException} (→ 403) si l'identité est absente.
 */
@Component
public class KeycloakCurrentActorProvider implements CurrentActorProvider {

    @Override
    public UUID requireActorId() {
        return CurrentUser.requireUserId();
    }
}
