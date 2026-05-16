package com.openlab.qualitos.quality.ehs.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.ehs.application.TenantProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter du port {@link TenantProvider} qui lit {@code TenantContext}
 * (peuplé en amont par le filtre de sécurité depuis le JWT).
 */
@Component
public class TenantContextProvider implements TenantProvider {

    @Override
    public UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
