package com.openlab.qualitos.quality.dashboards.export.web;

import com.openlab.qualitos.quality.dashboards.export.application.DashboardExportDto;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardExportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated endpoint to produce an official, signed &amp; anchored PDF export
 * of a custom dashboard (CLAUDE.md §7.3 / §7.4). Tenant comes from the JWT.
 *
 * <p>{@code POST /api/v1/dashboards/custom/{id}/export/pdf} → {@code application/pdf}.
 * Integrity metadata (verification code, SHA-256, anchor ref) is surfaced in
 * custom response headers so a client can persist/display the proof.
 */
@RestController
@RequestMapping("/api/v1/dashboards/custom")
@Validated
public class DashboardExportController {

    private final DashboardExportService service;

    public DashboardExportController(DashboardExportService service) {
        this.service = service;
    }

    @PostMapping(value = "/{id}/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID id,
                                            @Valid @RequestBody(required = false)
                                            DashboardExportWebDto.ExportRequest req) {
        List<DashboardExportDto.WidgetSnapshot> widgets =
                req == null || req.widgets() == null ? List.of()
                        : req.widgets().stream()
                            .map(w -> new DashboardExportDto.WidgetSnapshot(
                                    w.title(), w.type(), w.dataLines()))
                            .toList();

        DashboardExportDto.ExportResult result =
                service.export(id, new DashboardExportDto.ExportCommand(widgets));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", result.fileName());
        headers.add("X-Export-Verification-Code", result.verificationCode());
        headers.add("X-Export-Sha256", result.sha256Hex());
        headers.add("X-Export-Anchor-Ref", result.anchorTxRef());
        headers.setContentLength(result.pdf().length);
        return new ResponseEntity<>(result.pdf(), headers, 200);
    }
}
