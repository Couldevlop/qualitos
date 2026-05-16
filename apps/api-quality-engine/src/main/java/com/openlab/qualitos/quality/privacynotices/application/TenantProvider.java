package com.openlab.qualitos.quality.privacynotices.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
