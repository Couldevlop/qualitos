package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Tenant courant depuis le {@link TenantContext} (JWT validé, jamais du body). */
@Component("normDocTenantContextProvider")
public class NormDocTenantContextProvider implements NormDocTenantProvider {

    @Override
    public UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }
}
