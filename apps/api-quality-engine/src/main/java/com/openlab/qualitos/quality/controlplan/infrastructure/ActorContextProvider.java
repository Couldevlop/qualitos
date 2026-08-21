package com.openlab.qualitos.quality.controlplan.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.controlplan.application.ActorProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("controlPlanActorContextProvider")
public class ActorContextProvider implements ActorProvider {

    @Override
    public UUID currentUserId() {
        return CurrentUser.requireUserId();
    }
}
