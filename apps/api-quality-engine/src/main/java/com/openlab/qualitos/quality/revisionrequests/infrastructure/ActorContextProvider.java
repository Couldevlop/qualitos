package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.revisionrequests.application.ActorProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("revisionRequestActorContextProvider")
public class ActorContextProvider implements ActorProvider {

    @Override
    public UUID currentUserId() {
        return CurrentUser.requireUserId();
    }
}
