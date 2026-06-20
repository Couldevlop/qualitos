package com.openlab.qualitos.quality.dashboards.annotations.application;

import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotation;

import java.time.Instant;
import java.util.UUID;

public final class DashboardAnnotationDto {
    private DashboardAnnotationDto() {}

    public record CreateRequest(
            String chartKey,
            String anchorLabel,
            String body) {}

    public record View(
            UUID id,
            UUID tenantId,
            UUID authorId,
            String chartKey,
            String anchorLabel,
            String body,
            Instant createdAt,
            /** True if the requesting user may delete this annotation (author or admin). */
            boolean deletable) {

        public static View of(DashboardAnnotation a, boolean deletable) {
            return new View(a.getId(), a.getTenantId(), a.getAuthorId(),
                    a.getChartKey(), a.getAnchorLabel(), a.getBody(),
                    a.getCreatedAt(), deletable);
        }
    }
}
