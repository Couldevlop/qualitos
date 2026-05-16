package com.openlab.qualitos.quality.ropa.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
