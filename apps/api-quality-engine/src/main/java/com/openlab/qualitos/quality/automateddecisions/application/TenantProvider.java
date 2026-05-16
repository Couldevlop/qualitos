package com.openlab.qualitos.quality.automateddecisions.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
