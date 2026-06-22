package com.openlab.qualitos.quality.dashboards.annotations.infrastructure;

import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotation;

final class DashboardAnnotationMapper {
    private DashboardAnnotationMapper() {}

    static DashboardAnnotation toDomain(DashboardAnnotationJpaEntity e) {
        return new DashboardAnnotation(
                e.getId(), e.getTenantId(), e.getAuthorId(),
                e.getChartKey(), e.getAnchorLabel(), e.getBody(), e.getCreatedAt());
    }

    static DashboardAnnotationJpaEntity toEntity(DashboardAnnotation a) {
        DashboardAnnotationJpaEntity e = new DashboardAnnotationJpaEntity();
        e.setId(a.getId());
        e.setTenantId(a.getTenantId());
        e.setAuthorId(a.getAuthorId());
        e.setChartKey(a.getChartKey());
        e.setAnchorLabel(a.getAnchorLabel());
        e.setBody(a.getBody());
        e.setCreatedAt(a.getCreatedAt());
        return e;
    }
}
