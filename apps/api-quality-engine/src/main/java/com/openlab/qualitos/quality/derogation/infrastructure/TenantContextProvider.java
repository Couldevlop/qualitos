package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.derogation.application.TenantProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Le client vient du jeton valide, jamais du corps de la requete (section 18.2 #2). */
@Component("derogationTenantContextProvider")
public class TenantContextProvider implements TenantProvider {

    @Override
    public UUID requireTenantId() {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(TenantContext.getTenantId());
    }
}
