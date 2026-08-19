package com.openlab.qualitos.quality.product.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
