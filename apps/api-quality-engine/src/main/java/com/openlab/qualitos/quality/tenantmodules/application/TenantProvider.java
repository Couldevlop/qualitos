package com.openlab.qualitos.quality.tenantmodules.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
