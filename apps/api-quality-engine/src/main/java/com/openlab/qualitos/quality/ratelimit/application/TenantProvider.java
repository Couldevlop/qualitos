package com.openlab.qualitos.quality.ratelimit.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
