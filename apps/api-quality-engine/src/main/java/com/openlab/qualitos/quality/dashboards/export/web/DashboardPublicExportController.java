package com.openlab.qualitos.quality.dashboards.export.web;

import com.openlab.qualitos.quality.dashboards.export.application.DashboardExportDto;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardExportService;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (permitAll) verification of a signed dashboard export, reached by
 * scanning the QR code on the PDF (CLAUDE.md §7.4 — "QR code blockchain").
 *
 * <p>{@code GET /api/v1/dashboards/public/exports/{code}/verify}. No JWT, no
 * tenant context: the opaque random code is the authority (same model as Academy
 * certificate verification). The response carries ONLY integrity facts — never
 * tenant business data (OWASP A01: no data disclosure). An unknown code returns
 * a {@code valid=false} result, not an error, so the endpoint cannot be used to
 * enumerate tenants.
 */
@RestController
@RequestMapping("/api/v1/dashboards/public/exports")
@Validated
public class DashboardPublicExportController {

    private final DashboardExportService service;

    public DashboardPublicExportController(DashboardExportService service) {
        this.service = service;
    }

    @GetMapping("/{code}/verify")
    public DashboardExportDto.VerificationResult verify(
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$", message = "invalid verification code")
            String code) {
        return service.verify(code);
    }
}
