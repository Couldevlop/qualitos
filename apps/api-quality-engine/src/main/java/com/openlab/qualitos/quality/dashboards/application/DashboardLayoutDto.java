package com.openlab.qualitos.quality.dashboards.application;

import com.openlab.qualitos.quality.dashboards.domain.DashboardLayout;
import java.time.Instant;
import java.util.UUID;

public final class DashboardLayoutDto {
    private DashboardLayoutDto() {}

    public record SaveRequest(
            String name,
            String description,
            String layoutJson,
            boolean shared
    ) {}

    public record View(
            UUID id, UUID tenantId, UUID userId,
            String name, String description,
            String layoutJson, boolean shared,
            String signatureHash, int version,
            Instant createdAt, Instant updatedAt) {
        public static View of(DashboardLayout l) {
            return new View(l.getId(), l.getTenantId(), l.getUserId(),
                    l.getName(), l.getDescription(),
                    l.getLayoutJson(), l.isShared(),
                    l.getSignatureHash(), l.getVersion(),
                    l.getCreatedAt(), l.getUpdatedAt());
        }
    }
}
