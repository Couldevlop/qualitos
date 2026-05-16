package com.openlab.qualitos.quality.nis2measures.application;

import java.util.UUID;

public interface TenantProvider {
    UUID requireTenantId();
}
