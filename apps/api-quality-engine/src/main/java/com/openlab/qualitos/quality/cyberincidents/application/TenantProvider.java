package com.openlab.qualitos.quality.cyberincidents.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
