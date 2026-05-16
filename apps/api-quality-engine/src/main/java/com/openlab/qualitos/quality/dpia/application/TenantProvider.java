package com.openlab.qualitos.quality.dpia.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
