package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditTenantProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Tenant courant depuis le {@link TenantContext} (JWT validé, jamais du body). */
@Component("mockAuditTenantContextProvider")
public class MockAuditTenantContextProvider implements MockAuditTenantProvider {

    @Override
    public UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }
}
