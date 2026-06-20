package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocActorProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Acteur courant depuis le {@code sub} du JWT ({@link CurrentUser}). Garantit
 * que l'approbateur/soumetteur est l'identité authentifiée (OWASP A01, §18.2 #5).
 */
@Component("normDocCurrentUserActorProvider")
public class NormDocCurrentUserActorProvider implements NormDocActorProvider {

    @Override
    public UUID requireActorId() {
        return CurrentUser.requireUserId();
    }
}
