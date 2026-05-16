package com.openlab.qualitos.quality.aiincidents.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
