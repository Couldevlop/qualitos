package com.openlab.qualitos.quality.controlplan.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
