package com.openlab.qualitos.quality.gdpr.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
