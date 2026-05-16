package com.openlab.qualitos.quality.retention.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
