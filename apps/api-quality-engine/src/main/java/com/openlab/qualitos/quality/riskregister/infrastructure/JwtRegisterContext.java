package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.riskregister.application.RegisterContext;

import java.util.Optional;
import java.util.UUID;

/** Le tenant et l'acteur viennent du JETON validé, jamais du corps (§18.2). */
public class JwtRegisterContext implements RegisterContext {

    private final RecordScope scope;

    public JwtRegisterContext(RecordScope scope) {
        this.scope = scope;
    }

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
    public Optional<UUID> visibleOnlyTo() {
        return scope.restrictTo(Permission.RISK_VIEW_ALL);
    }
}
