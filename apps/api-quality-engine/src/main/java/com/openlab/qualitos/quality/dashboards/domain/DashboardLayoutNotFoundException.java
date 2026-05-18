package com.openlab.qualitos.quality.dashboards.domain;

import java.util.UUID;

public class DashboardLayoutNotFoundException extends RuntimeException {
    public DashboardLayoutNotFoundException(UUID id) {
        super("Dashboard layout not found: " + id);
    }
}
