package com.openlab.qualitos.quality.costofquality.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.costofquality.application.CoqContext;

import java.util.UUID;

/** Le tenant et l'acteur viennent du JETON validé, jamais du corps (§18.2). */
public class JwtCoqContext implements CoqContext {

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
