package com.openlab.qualitos.quality.aipmm.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
