package com.openlab.qualitos.quality.processoragreements.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
