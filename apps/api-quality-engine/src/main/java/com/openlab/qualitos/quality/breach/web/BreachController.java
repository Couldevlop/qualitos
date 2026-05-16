package com.openlab.qualitos.quality.breach.web;

import com.openlab.qualitos.quality.breach.application.BreachDto;
import com.openlab.qualitos.quality.breach.application.BreachService;
import com.openlab.qualitos.quality.breach.domain.BreachStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints — incidents de violation RGPD (Art. 33/34).
 */
@RestController
@RequestMapping("/api/v1/gdpr/breaches")
@Validated
public class BreachController {

    private final BreachService service;

    public BreachController(BreachService service) { this.service = service; }

    @GetMapping
    public List<BreachDto.View> list(@RequestParam(required = false) BreachStatus status) {
        return service.list(status);
    }

    @GetMapping("/dpa-overdue")
    public List<BreachDto.View> dpaOverdue(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return service.dpaOverdue(limit);
    }

    @GetMapping("/{id}")
    public BreachDto.View get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BreachDto.View detect(@Valid @RequestBody BreachWebDto.DetectRequest req) {
        return service.detect(new BreachDto.DetectRequest(
                req.internalReference(), req.title(), req.description(),
                req.detectedAt(), req.occurredAt(),
                req.severity(), req.affectedSubjectsCount(),
                req.affectedDataCategories(), req.riskOfHarmDescription(),
                req.reportedByUserId()));
    }

    @PostMapping("/{id}/start-assessment")
    public BreachDto.View startAssessment(@PathVariable UUID id,
                                          @Valid @RequestBody BreachWebDto.StartAssessmentRequest req) {
        return service.startAssessment(id,
                new BreachDto.StartAssessmentRequest(req.handledByUserId()));
    }

    @PostMapping("/{id}/contain")
    public BreachDto.View contain(@PathVariable UUID id,
                                  @Valid @RequestBody BreachWebDto.ContainRequest req) {
        return service.contain(id,
                new BreachDto.ContainRequest(req.containmentMeasures(), req.handledByUserId()));
    }

    @PostMapping("/{id}/notify-dpa")
    public BreachDto.View notifyDpa(@PathVariable UUID id,
                                    @Valid @RequestBody BreachWebDto.DpaNotificationRequest req) {
        return service.notifyDpa(id,
                new BreachDto.DpaNotificationRequest(req.notifiedAt(), req.reference()));
    }

    @PostMapping("/{id}/notify-subjects")
    public BreachDto.View notifySubjects(@PathVariable UUID id,
                                         @Valid @RequestBody BreachWebDto.SubjectsNotificationRequest req) {
        return service.notifySubjects(id,
                new BreachDto.SubjectsNotificationRequest(req.notifiedAt(), req.channel()));
    }

    @PostMapping("/{id}/close")
    public BreachDto.View close(@PathVariable UUID id,
                                @Valid @RequestBody BreachWebDto.CloseRequest req) {
        return service.close(id, new BreachDto.CloseRequest(req.closureNotes()));
    }

    @PostMapping("/{id}/reject")
    public BreachDto.View reject(@PathVariable UUID id,
                                 @Valid @RequestBody BreachWebDto.RejectRequest req) {
        return service.reject(id, new BreachDto.RejectRequest(req.reason()));
    }

    @PostMapping("/{id}/severity")
    public BreachDto.View updateSeverity(@PathVariable UUID id,
                                         @Valid @RequestBody BreachWebDto.UpdateSeverityRequest req) {
        return service.updateSeverity(id,
                new BreachDto.UpdateSeverityRequest(req.severity()));
    }
}
