package com.openlab.qualitos.quality.aiincidents.web;

import com.openlab.qualitos.quality.aiincidents.application.AiIncidentDto;
import com.openlab.qualitos.quality.aiincidents.application.AiIncidentService;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentSeverity;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints — registre des incidents IA AI Act (Art. 73).
 */
@RestController
@RequestMapping("/api/v1/ai-act/incidents")
@Validated
public class AiIncidentController {

    private final AiIncidentService service;

    public AiIncidentController(AiIncidentService service) { this.service = service; }

    @GetMapping
    public List<AiIncidentDto.View> list(@RequestParam(required = false) AiIncidentStatus status) {
        return service.list(status);
    }

    @GetMapping("/by-system")
    public List<AiIncidentDto.View> byAiSystem(@RequestParam @NotNull UUID aiSystemId) {
        return service.listByAiSystem(aiSystemId);
    }

    @GetMapping("/by-severity")
    public List<AiIncidentDto.View> bySeverity(@RequestParam @NotNull AiIncidentSeverity severity) {
        return service.listBySeverity(severity);
    }

    @GetMapping("/overdue-regulator-notification")
    public List<AiIncidentDto.View> overdue(
            @RequestParam(defaultValue = "200") @Min(1) @Max(1000) int limit) {
        return service.listOverdueForRegulator(limit);
    }

    @GetMapping("/{id}")
    public AiIncidentDto.View get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/by-reference")
    public AiIncidentDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AiIncidentDto.View detect(@Valid @RequestBody AiIncidentWebDto.DetectRequest req) {
        return service.detect(new AiIncidentDto.DetectRequest(
                req.reference(), req.aiSystemId(), req.severity(),
                req.description(), req.affectedPersonsDescription(),
                req.immediateActionsTaken(),
                req.occurredAt(), req.detectedAt(), req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public AiIncidentDto.View edit(@PathVariable UUID id,
                                   @Valid @RequestBody AiIncidentWebDto.EditRequest req) {
        return service.edit(id, new AiIncidentDto.EditRequest(
                req.description(), req.affectedPersonsDescription(),
                req.immediateActionsTaken()));
    }

    @PostMapping("/{id}/start-investigation")
    public AiIncidentDto.View startInvestigation(@PathVariable UUID id,
            @Valid @RequestBody AiIncidentWebDto.StartInvestigationRequest req) {
        return service.startInvestigation(id,
                new AiIncidentDto.StartInvestigationRequest(req.investigationLeadUserId()));
    }

    @PostMapping("/{id}/notify-regulator")
    public AiIncidentDto.View notifyRegulator(@PathVariable UUID id,
            @Valid @RequestBody AiIncidentWebDto.NotifyRegulatorRequest req) {
        return service.notifyRegulator(id, new AiIncidentDto.NotifyRegulatorRequest(
                req.regulatorReference(), req.rootCauseAnalysis(), req.correctiveActions()));
    }

    @PostMapping("/{id}/close")
    public AiIncidentDto.View close(@PathVariable UUID id,
            @Valid @RequestBody AiIncidentWebDto.CloseRequest req) {
        return service.close(id, new AiIncidentDto.CloseRequest(req.correctiveActions()));
    }

    @PostMapping("/{id}/dismiss")
    public AiIncidentDto.View dismiss(@PathVariable UUID id,
            @Valid @RequestBody AiIncidentWebDto.DismissRequest req) {
        return service.dismiss(id, new AiIncidentDto.DismissRequest(req.reason()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
