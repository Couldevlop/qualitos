package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.derogation.application.ActorProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** L'acteur vient du jeton valide : c'est lui que les gardes du domaine comparent. */
@Component("derogationActorContextProvider")
public class ActorContextProvider implements ActorProvider {

    @Override
    public UUID requireAccountId() {
        return CurrentUser.requireUserId();
    }

    @Override
    public Optional<String> displayName() {
        return CurrentUser.displayName();
    }
}
