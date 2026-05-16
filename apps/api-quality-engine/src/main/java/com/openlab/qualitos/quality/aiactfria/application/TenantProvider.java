package com.openlab.qualitos.quality.aiactfria.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
