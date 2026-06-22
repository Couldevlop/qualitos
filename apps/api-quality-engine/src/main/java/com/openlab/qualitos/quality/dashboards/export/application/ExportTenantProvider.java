package com.openlab.qualitos.quality.dashboards.export.application;

import java.util.UUID;

/** Port — tenant + user id from the JWT (never the body, §18.2 #2). */
public interface ExportTenantProvider {
    UUID requireTenantId();
    UUID requireUserId();
}
