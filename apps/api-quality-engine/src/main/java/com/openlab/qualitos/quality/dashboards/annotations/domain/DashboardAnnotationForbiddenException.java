package com.openlab.qualitos.quality.dashboards.annotations.domain;

import java.util.UUID;

/**
 * Raised when a user tries to delete an annotation they did not author
 * and is not allowed to manage (only author or tenant admin can delete).
 */
public class DashboardAnnotationForbiddenException extends RuntimeException {
    public DashboardAnnotationForbiddenException(UUID id) {
        super("Not allowed to delete annotation: " + id);
    }
}
