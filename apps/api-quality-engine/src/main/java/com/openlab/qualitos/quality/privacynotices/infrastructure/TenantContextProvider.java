package com.openlab.qualitos.quality.privacynotices.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.privacynotices.application.TenantProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("privacyNoticesTenantContextProvider")
public class TenantContextProvider implements TenantProvider {

    @Override
    public UUID requireTenantId() {
        if (!TenantContext.hasTenant()) throw new MissingTenantContextException();
        return UUID.fromString(TenantContext.getTenantId());
    }
}
