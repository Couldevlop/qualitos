package com.openlab.qualitos.quality.dashboards.export.application;

import java.util.UUID;

/**
 * Port — loads the minimal, tenant-scoped facts about a dashboard layout the
 * exporter needs (its name), enforcing visibility (owner OR shared within
 * tenant). Cross-tenant / not-visible access surfaces as not-found upstream.
 *
 * <p>Keeps the export application module decoupled from the layout module's own
 * service (which is Spring-wired) while preserving the multi-tenant invariant.
 */
public interface DashboardLayoutLoaderPort {

    /** @return the dashboard name (visibility enforced by the adapter). */
    String requireVisibleName(UUID dashboardId);
}
