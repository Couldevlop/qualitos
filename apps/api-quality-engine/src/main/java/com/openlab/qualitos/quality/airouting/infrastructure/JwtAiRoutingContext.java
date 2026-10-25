package com.openlab.qualitos.quality.airouting.infrastructure;

import com.openlab.qualitos.quality.airouting.application.AiRoutingContext;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;

import java.util.UUID;

/** Le tenant et l'acteur viennent du JETON validé, jamais du corps (§18.2). */
public class JwtAiRoutingContext implements AiRoutingContext {

    @Override
    public UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }

    @Override
    public UUID requireActorId() {
        return CurrentUser.requireUserId();
    }
}
