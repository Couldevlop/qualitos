package com.openlab.qualitos.quality.dashboards.annotations.domain;

import java.util.UUID;

/** Raised when an annotation is absent OR belongs to another tenant (A01 — no leak). */
public class DashboardAnnotationNotFoundException extends RuntimeException {
    public DashboardAnnotationNotFoundException(UUID id) {
        super("Dashboard annotation not found: " + id);
    }
}
