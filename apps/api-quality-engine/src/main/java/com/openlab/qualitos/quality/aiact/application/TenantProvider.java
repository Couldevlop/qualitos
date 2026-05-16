package com.openlab.qualitos.quality.aiact.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
