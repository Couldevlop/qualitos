package com.openlab.qualitos.quality.nis2measures.web;

import com.openlab.qualitos.quality.nis2measures.application.Nis2MeasureDto;
import com.openlab.qualitos.quality.nis2measures.application.Nis2MeasureService;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureCategory;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStatus;
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
 * Endpoints — mesures NIS2 Art. 21.
 */
@RestController
@RequestMapping("/api/v1/nis2/risk-measures")
@Validated
public class Nis2MeasureController {

    private final Nis2MeasureService service;

    public Nis2MeasureController(Nis2MeasureService service) { this.service = service; }

    @GetMapping
    public List<Nis2MeasureDto.View> list(@RequestParam(required = false) Nis2MeasureStatus status) {
        return service.list(status);
    }

    @GetMapping("/by-category")
    public List<Nis2MeasureDto.View> byCategory(@RequestParam @NotNull Nis2MeasureCategory category) {
        return service.listByCategory(category);
    }

    @GetMapping("/{id}")
    public Nis2MeasureDto.View get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/by-reference")
    public Nis2MeasureDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @GetMapping("/review-overdue")
    public List<Nis2MeasureDto.View> reviewOverdue(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        return service.reviewOverdue(limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Nis2MeasureDto.View plan(@Valid @RequestBody Nis2MeasureWebDto.PlanRequest req) {
        return service.plan(new Nis2MeasureDto.PlanRequest(
                req.reference(), req.category(), req.title(), req.description(),
                req.ownerUserId(), req.maturityLevel(),
                req.residualRiskRating(), req.criticalRiskJustification(),
                req.reviewIntervalDays(),
                req.evidenceUrls(),
                req.linkedProcessingActivityIds(),
                req.linkedProcessorAgreementIds(),
                req.notes(), req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public Nis2MeasureDto.View edit(@PathVariable UUID id,
                                    @Valid @RequestBody Nis2MeasureWebDto.EditRequest req) {
        return service.edit(id, new Nis2MeasureDto.EditRequest(
                req.title(), req.description(),
                req.ownerUserId(), req.maturityLevel(),
                req.residualRiskRating(), req.criticalRiskJustification(),
                req.reviewIntervalDays(),
                req.evidenceUrls(),
                req.linkedProcessingActivityIds(),
                req.linkedProcessorAgreementIds(),
                req.notes()));
    }

    @PostMapping("/{id}/start")
    public Nis2MeasureDto.View startImplementation(@PathVariable UUID id) {
        return service.startImplementation(id);
    }

    @PostMapping("/{id}/implemented")
    public Nis2MeasureDto.View markImplemented(@PathVariable UUID id) {
        return service.markImplemented(id);
    }

    @PostMapping("/{id}/verify")
    public Nis2MeasureDto.View verify(@PathVariable UUID id,
                                      @Valid @RequestBody Nis2MeasureWebDto.VerifyRequest req) {
        return service.verify(id,
                new Nis2MeasureDto.VerifyRequest(req.reviewedByUserId(), req.reviewedAt()));
    }

    @PostMapping("/{id}/review")
    public Nis2MeasureDto.View review(@PathVariable UUID id,
                                      @Valid @RequestBody Nis2MeasureWebDto.ReviewRequest req) {
        return service.review(id,
                new Nis2MeasureDto.ReviewRequest(req.reviewedByUserId(), req.reviewedAt()));
    }

    @PostMapping("/{id}/deprecate")
    public Nis2MeasureDto.View deprecate(@PathVariable UUID id) {
        return service.deprecate(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
