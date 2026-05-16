package com.openlab.qualitos.quality.cyberincidents.web;

import com.openlab.qualitos.quality.cyberincidents.application.CyberIncidentDto;
import com.openlab.qualitos.quality.cyberincidents.application.CyberIncidentService;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints — incidents cyber NIS2 (Art. 23).
 */
@RestController
@RequestMapping("/api/v1/nis2/cyber-incidents")
@Validated
public class CyberIncidentController {

    private final CyberIncidentService service;

    public CyberIncidentController(CyberIncidentService service) { this.service = service; }

    @GetMapping
    public List<CyberIncidentDto.View> list(@RequestParam(required = false) CyberIncidentStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public CyberIncidentDto.View get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/by-reference")
    public CyberIncidentDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @GetMapping("/early-warning-overdue")
    public List<CyberIncidentDto.View> earlyWarningOverdue(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return service.earlyWarningOverdue(limit);
    }

    @GetMapping("/initial-assessment-overdue")
    public List<CyberIncidentDto.View> initialAssessmentOverdue(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return service.initialAssessmentOverdue(limit);
    }

    @GetMapping("/final-report-overdue")
    public List<CyberIncidentDto.View> finalReportOverdue(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return service.finalReportOverdue(limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CyberIncidentDto.View detect(@Valid @RequestBody CyberIncidentWebDto.DetectRequest req) {
        return service.detect(new CyberIncidentDto.DetectRequest(
                req.reference(), req.title(), req.description(),
                req.detectedAt(), req.occurredAt(),
                req.incidentType(), req.severity(),
                req.estimatedAffectedUsers(),
                req.affectedAssets(), req.affectedServices(),
                req.linkedBreachId(), req.reportedByUserId()));
    }

    @PostMapping("/{id}/start-assessment")
    public CyberIncidentDto.View startAssessment(@PathVariable UUID id,
                                                 @Valid @RequestBody CyberIncidentWebDto.StartAssessmentRequest req) {
        return service.startAssessment(id,
                new CyberIncidentDto.StartAssessmentRequest(req.handledByUserId()));
    }

    @PostMapping("/{id}/mitigate")
    public CyberIncidentDto.View mitigate(@PathVariable UUID id,
                                          @Valid @RequestBody CyberIncidentWebDto.MitigateRequest req) {
        return service.mitigate(id, new CyberIncidentDto.MitigateRequest(
                req.containmentMeasures(), req.impactDescription(), req.handledByUserId()));
    }

    @PostMapping("/{id}/early-warning")
    public CyberIncidentDto.View recordEarlyWarning(@PathVariable UUID id,
                                                    @Valid @RequestBody CyberIncidentWebDto.NotificationRequest req) {
        return service.recordEarlyWarning(id,
                new CyberIncidentDto.NotificationRequest(req.sentAt(), req.reference()));
    }

    @PostMapping("/{id}/initial-assessment")
    public CyberIncidentDto.View recordInitialAssessment(@PathVariable UUID id,
                                                         @Valid @RequestBody CyberIncidentWebDto.NotificationRequest req) {
        return service.recordInitialAssessment(id,
                new CyberIncidentDto.NotificationRequest(req.sentAt(), req.reference()));
    }

    @PostMapping("/{id}/final-report")
    public CyberIncidentDto.View recordFinalReport(@PathVariable UUID id,
                                                   @Valid @RequestBody CyberIncidentWebDto.NotificationRequest req) {
        return service.recordFinalReport(id,
                new CyberIncidentDto.NotificationRequest(req.sentAt(), req.reference()));
    }

    @PostMapping("/{id}/close")
    public CyberIncidentDto.View close(@PathVariable UUID id,
                                       @Valid @RequestBody CyberIncidentWebDto.CloseRequest req) {
        return service.close(id, new CyberIncidentDto.CloseRequest(req.closureNotes()));
    }

    @PostMapping("/{id}/reject")
    public CyberIncidentDto.View reject(@PathVariable UUID id,
                                        @Valid @RequestBody CyberIncidentWebDto.RejectRequest req) {
        return service.reject(id, new CyberIncidentDto.RejectRequest(req.reason()));
    }

    @PostMapping("/{id}/severity")
    public CyberIncidentDto.View updateSeverity(@PathVariable UUID id,
                                                @Valid @RequestBody CyberIncidentWebDto.UpdateSeverityRequest req) {
        return service.updateSeverity(id,
                new CyberIncidentDto.UpdateSeverityRequest(req.severity()));
    }

    @PostMapping("/{id}/link-breach")
    public CyberIncidentDto.View linkBreach(@PathVariable UUID id,
                                            @Valid @RequestBody CyberIncidentWebDto.LinkBreachRequest req) {
        return service.linkBreach(id, new CyberIncidentDto.LinkBreachRequest(req.breachId()));
    }
}
