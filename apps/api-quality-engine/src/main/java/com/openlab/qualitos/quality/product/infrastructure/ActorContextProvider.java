package com.openlab.qualitos.quality.product.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.product.application.ActorProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ActorContextProvider implements ActorProvider {

    @Override
    public UUID currentUserId() {
        return CurrentUser.requireUserId();
    }
}
