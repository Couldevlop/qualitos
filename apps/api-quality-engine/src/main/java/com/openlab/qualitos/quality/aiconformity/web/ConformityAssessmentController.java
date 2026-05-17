package com.openlab.qualitos.quality.aiconformity.web;

import com.openlab.qualitos.quality.aiconformity.application.ConformityAssessmentDto;
import com.openlab.qualitos.quality.aiconformity.application.ConformityAssessmentService;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStatus;
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
 * Endpoints — évaluations de conformité AI Act (Art. 43).
 */
@RestController
@RequestMapping("/api/v1/ai-act/conformity-assessments")
@Validated
public class ConformityAssessmentController {

    private final ConformityAssessmentService service;

    public ConformityAssessmentController(ConformityAssessmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConformityAssessmentDto.View> list(
            @RequestParam(required = false) ConformityAssessmentStatus status) {
        return service.list(status);
    }

    @GetMapping("/by-system")
    public List<ConformityAssessmentDto.View> byAiSystem(
            @RequestParam @NotNull UUID aiSystemId) {
        return service.listByAiSystem(aiSystemId);
    }

    @GetMapping("/expiring-certificates")
    public List<ConformityAssessmentDto.View> expiring(
            @RequestParam(defaultValue = "200") @Min(1) @Max(1000) int limit) {
        return service.listExpiringCertificates(limit);
    }

    @GetMapping("/{id}")
    public ConformityAssessmentDto.View get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/by-reference")
    public ConformityAssessmentDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConformityAssessmentDto.View plan(
            @Valid @RequestBody ConformityAssessmentWebDto.PlanRequest req) {
        return service.plan(new ConformityAssessmentDto.PlanRequest(
                req.reference(), req.aiSystemId(), req.qmsId(), req.procedure(),
                req.notifiedBodyId(), req.notifiedBodyName(), req.scope(),
                req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public ConformityAssessmentDto.View edit(@PathVariable UUID id,
            @Valid @RequestBody ConformityAssessmentWebDto.EditRequest req) {
        return service.edit(id, new ConformityAssessmentDto.EditRequest(
                req.qmsId(), req.notifiedBodyId(), req.notifiedBodyName(), req.scope()));
    }

    @PostMapping("/{id}/start")
    public ConformityAssessmentDto.View start(@PathVariable UUID id) {
        return service.start(id);
    }

    @PostMapping("/{id}/certify")
    public ConformityAssessmentDto.View certify(@PathVariable UUID id,
            @Valid @RequestBody ConformityAssessmentWebDto.CertifyRequest req) {
        return service.certify(id, new ConformityAssessmentDto.CertifyRequest(
                req.certificateNumber(), req.euDeclarationReference(), req.validUntil()));
    }

    @PostMapping("/{id}/mark-expired")
    public ConformityAssessmentDto.View markExpired(@PathVariable UUID id) {
        return service.markExpired(id);
    }

    @PostMapping("/{id}/revoke")
    public ConformityAssessmentDto.View revoke(@PathVariable UUID id,
            @Valid @RequestBody ConformityAssessmentWebDto.RevokeRequest req) {
        return service.revoke(id, new ConformityAssessmentDto.RevokeRequest(req.reason()));
    }

    @PostMapping("/{id}/fail")
    public ConformityAssessmentDto.View markFailed(@PathVariable UUID id,
            @Valid @RequestBody ConformityAssessmentWebDto.FailRequest req) {
        return service.markFailed(id, new ConformityAssessmentDto.FailRequest(req.reason()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
