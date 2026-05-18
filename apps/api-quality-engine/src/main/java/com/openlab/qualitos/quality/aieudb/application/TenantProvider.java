package com.openlab.qualitos.quality.aieudb.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
