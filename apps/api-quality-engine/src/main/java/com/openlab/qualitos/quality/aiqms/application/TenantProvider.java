package com.openlab.qualitos.quality.aiqms.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
