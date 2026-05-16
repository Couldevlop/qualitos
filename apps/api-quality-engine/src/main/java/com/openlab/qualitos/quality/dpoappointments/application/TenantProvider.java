package com.openlab.qualitos.quality.dpoappointments.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
