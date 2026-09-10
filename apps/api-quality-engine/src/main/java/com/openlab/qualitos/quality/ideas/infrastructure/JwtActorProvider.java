package com.openlab.qualitos.quality.ideas.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.ideas.application.ActorProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * L'acteur vient du JETON, jamais du corps.
 *
 * <p>C'est l'invariant §18.2 : une attribution que l'appelant pourrait écrire
 * lui-même permet de déposer une idée au nom d'un autre, ou de se déclarer
 * validateur de sa propre idée — et la garde « le validateur n'est pas le
 * proposeur » ne comparerait alors que deux valeurs fournies par le même client.
 */
@Component("ideasJwtActorProvider")
public class JwtActorProvider implements ActorProvider {

    @Override
    public UUID requireActorId() {
        return CurrentUser.requireUserId();
    }

    @Override
    public java.util.Optional<String> actorDisplayName() {
        return CurrentUser.displayName();
    }
}
