package com.openlab.qualitos.quality.breach.infrastructure;

import com.openlab.qualitos.quality.breach.application.TenantProvider;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("breachTenantContextProvider")
public class TenantContextProvider implements TenantProvider {

    @Override
    public UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
