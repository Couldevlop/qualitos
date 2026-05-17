package com.openlab.qualitos.quality.aiconformity.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
