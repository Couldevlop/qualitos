package com.openlab.qualitos.quality.revisionrequests.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
