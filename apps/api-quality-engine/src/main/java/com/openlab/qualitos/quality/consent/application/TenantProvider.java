package com.openlab.qualitos.quality.consent.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
