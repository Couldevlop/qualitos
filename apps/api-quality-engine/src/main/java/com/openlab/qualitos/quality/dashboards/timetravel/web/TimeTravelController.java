package com.openlab.qualitos.quality.dashboards.timetravel.web;

import com.openlab.qualitos.quality.dashboards.timetravel.application.TimeTravelDto;
import com.openlab.qualitos.quality.dashboards.timetravel.application.TimeTravelService;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Dashboard time-travel (CLAUDE.md §7.3) — "show the dashboard state at …".
 *
 * <p>tenantId is resolved from the JWT; the response is a real as-of snapshot of
 * the tenant's KPI measurements at the requested instant.</p>
 */
@RestController
@RequestMapping("/api/v1/dashboards/time-travel")
@Validated
@PreAuthorize("isAuthenticated()")
public class TimeTravelController {

    private final TimeTravelService service;

    public TimeTravelController(TimeTravelService service) {
        this.service = service;
    }

    @GetMapping("/kpis")
    public TimeTravelDto.DashboardSnapshotView kpisAsOf(
            @RequestParam("asOf")
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant asOf) {
        return service.snapshotAsOf(asOf);
    }
}
