package com.openlab.qualitos.quality.dashboards.export.infrastructure;

import com.openlab.qualitos.quality.dashboards.application.DashboardLayoutService;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardLayoutLoaderPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Bridges the export module to the existing tenant-scoped dashboard layout
 * use-case. {@link DashboardLayoutService#get(UUID)} enforces visibility
 * (owner OR shared within tenant) and throws {@code DashboardLayoutNotFoundException}
 * on cross-tenant / private access — so the export inherits the exact same
 * multi-tenant guarantees without duplicating the rules.
 */
@Component
public class DashboardLayoutLoaderAdapter implements DashboardLayoutLoaderPort {

    private final DashboardLayoutService layoutService;

    public DashboardLayoutLoaderAdapter(DashboardLayoutService layoutService) {
        this.layoutService = layoutService;
    }

    @Override
    public String requireVisibleName(UUID dashboardId) {
        return layoutService.get(dashboardId).name();
    }
}
