package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderPorts;

import java.util.UUID;

/** Le client et l'acteur viennent du JETON validé, jamais du corps (§18.2 #2). */
public class JwtStakeholderContext implements StakeholderPorts.Context {

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

    @Override
    public String actorName() {
        return CurrentUser.displayName().orElse(null);
    }
}
