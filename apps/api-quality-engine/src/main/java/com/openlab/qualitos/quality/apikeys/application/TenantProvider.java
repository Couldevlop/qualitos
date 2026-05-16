package com.openlab.qualitos.quality.apikeys.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
