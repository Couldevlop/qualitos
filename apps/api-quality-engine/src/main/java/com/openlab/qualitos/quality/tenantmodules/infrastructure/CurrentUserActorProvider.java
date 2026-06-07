package com.openlab.qualitos.quality.tenantmodules.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.tenantmodules.application.ActorProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter résolvant l'acteur courant depuis le {@code sub} du JWT
 * ({@link CurrentUser}). Branche le port {@link ActorProvider} sur l'identité
 * authentifiée (H2).
 */
@Component("tenantModulesCurrentUserActorProvider")
public class CurrentUserActorProvider implements ActorProvider {

    @Override
    public UUID requireActorId() {
        return CurrentUser.requireUserId();
    }
}
