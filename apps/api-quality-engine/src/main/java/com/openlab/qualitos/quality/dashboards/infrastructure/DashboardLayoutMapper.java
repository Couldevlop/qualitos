package com.openlab.qualitos.quality.dashboards.infrastructure;

import com.openlab.qualitos.quality.dashboards.domain.DashboardLayout;

final class DashboardLayoutMapper {
    private DashboardLayoutMapper() {}

    static DashboardLayout toDomain(DashboardLayoutJpaEntity e) {
        return new DashboardLayout(
                e.getId(), e.getTenantId(), e.getUserId(),
                e.getName(), e.getDescription(),
                e.getLayoutJson(), e.isShared(),
                e.getSignatureHash(), e.getVersion(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    static DashboardLayoutJpaEntity toEntity(DashboardLayout d) {
        DashboardLayoutJpaEntity e = new DashboardLayoutJpaEntity();
        e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setUserId(d.getUserId());
        e.setName(d.getName());
        e.setDescription(d.getDescription());
        e.setLayoutJson(d.getLayoutJson());
        e.setShared(d.isShared());
        e.setSignatureHash(d.getSignatureHash());
        e.setVersion(d.getVersion());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }
}
