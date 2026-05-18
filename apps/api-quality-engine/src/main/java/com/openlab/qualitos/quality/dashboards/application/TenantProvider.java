package com.openlab.qualitos.quality.dashboards.application;

import java.util.UUID;

/** Port — tenant + user id from JWT (never body). */
public interface TenantProvider {
    UUID requireTenantId();
    UUID requireUserId();
}
