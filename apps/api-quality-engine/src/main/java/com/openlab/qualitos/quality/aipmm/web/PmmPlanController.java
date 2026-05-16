package com.openlab.qualitos.quality.aipmm.web;

import com.openlab.qualitos.quality.aipmm.application.PmmPlanDto;
import com.openlab.qualitos.quality.aipmm.application.PmmPlanService;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStatus;
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
 * Endpoints — plans de surveillance post-marché AI Act (Art. 72).
 */
@RestController
@RequestMapping("/api/v1/ai-act/pmm")
@Validated
public class PmmPlanController {

    private final PmmPlanService service;

    public PmmPlanController(PmmPlanService service) { this.service = service; }

    @GetMapping
    public List<PmmPlanDto.View> list(@RequestParam(required = false) PmmPlanStatus status) {
        return service.list(status);
    }

    @GetMapping("/by-system")
    public List<PmmPlanDto.View> byAiSystem(@RequestParam @NotNull UUID aiSystemId) {
        return service.listByAiSystem(aiSystemId);
    }

    @GetMapping("/overdue-reviews")
    public List<PmmPlanDto.View> overdueReviews(
            @RequestParam(defaultValue = "200") @Min(1) @Max(1000) int limit) {
        return service.listOverdueReviews(limit);
    }

    @GetMapping("/{id}")
    public PmmPlanDto.View get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/by-reference")
    public PmmPlanDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PmmPlanDto.View draft(@Valid @RequestBody PmmPlanWebDto.DraftRequest req) {
        return service.draft(new PmmPlanDto.DraftRequest(
                req.reference(), req.aiSystemId(), req.name(), req.description(),
                req.metricsMonitored(), req.collectionMethod(), req.reviewFrequency(),
                req.responsiblePartyDescription(), req.triggerCriteria(),
                req.qmsLinkReference(), req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public PmmPlanDto.View edit(@PathVariable UUID id,
                                @Valid @RequestBody PmmPlanWebDto.EditRequest req) {
        return service.edit(id, new PmmPlanDto.EditRequest(
                req.name(), req.description(),
                req.metricsMonitored(), req.collectionMethod(), req.reviewFrequency(),
                req.responsiblePartyDescription(), req.triggerCriteria(),
                req.qmsLinkReference()));
    }

    @PostMapping("/{id}/activate")
    public PmmPlanDto.View activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/record-review")
    public PmmPlanDto.View recordReview(@PathVariable UUID id,
                                        @Valid @RequestBody PmmPlanWebDto.ReviewRequest req) {
        return service.recordReview(id, new PmmPlanDto.ReviewRequest(req.reviewedByUserId()));
    }

    @PostMapping("/{id}/suspend")
    public PmmPlanDto.View suspend(@PathVariable UUID id,
                                   @Valid @RequestBody PmmPlanWebDto.SuspendRequest req) {
        return service.suspend(id, new PmmPlanDto.SuspendRequest(req.reason()));
    }

    @PostMapping("/{id}/close")
    public PmmPlanDto.View close(@PathVariable UUID id,
                                 @Valid @RequestBody PmmPlanWebDto.CloseRequest req) {
        return service.close(id, new PmmPlanDto.CloseRequest(req.reason()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
