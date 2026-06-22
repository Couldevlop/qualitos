package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.marketplace.application.TenantProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implémentation du port {@link TenantProvider} : le tenant courant est lu depuis
 * le {@link TenantContext} (alimenté par le filtre JWT). Lève si absent (→ 403).
 */
@Component
public class TenantContextTenantProvider implements TenantProvider {

    @Override
    public UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }
}
