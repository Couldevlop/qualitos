package com.openlab.qualitos.quality.crossbordertransfers.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
