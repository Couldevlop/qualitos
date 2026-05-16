package com.openlab.qualitos.quality.processoragreements.web;

import com.openlab.qualitos.quality.processoragreements.application.ProcessorAgreementDto;
import com.openlab.qualitos.quality.processoragreements.application.ProcessorAgreementService;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStatus;
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
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints — accords de sous-traitance RGPD (Art. 28).
 */
@RestController
@RequestMapping("/api/v1/gdpr/processor-agreements")
@Validated
public class ProcessorAgreementController {

    private final ProcessorAgreementService service;

    public ProcessorAgreementController(ProcessorAgreementService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProcessorAgreementDto.View> list(
            @RequestParam(required = false) ProcessorAgreementStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public ProcessorAgreementDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/by-reference")
    public ProcessorAgreementDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProcessorAgreementDto.View create(
            @Valid @RequestBody ProcessorAgreementWebDto.CreateRequest req) {
        return service.create(new ProcessorAgreementDto.CreateRequest(
                req.reference(), req.processorName(), req.processorLegalEntity(),
                req.processorContact(), req.processorDpoContact(),
                req.processorCountry(), req.servicesDescription(),
                req.subProcessorCategories(), req.linkedProcessingActivityIds(),
                req.thirdCountryTransfers(), req.transferSafeguards(),
                req.contractDocumentUrl(),
                req.signedAt(), req.effectiveFrom(), req.expirationDate(),
                req.securityMeasures(), req.breachNotificationCommitmentHours(),
                req.auditRights(), req.auditRightsNotes(),
                req.dataReturnOrDeletionTerms(), req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public ProcessorAgreementDto.View edit(
            @PathVariable UUID id,
            @Valid @RequestBody ProcessorAgreementWebDto.EditRequest req) {
        return service.edit(id, new ProcessorAgreementDto.EditRequest(
                req.processorName(), req.processorLegalEntity(),
                req.processorContact(), req.processorDpoContact(),
                req.processorCountry(), req.servicesDescription(),
                req.subProcessorCategories(), req.linkedProcessingActivityIds(),
                req.thirdCountryTransfers(), req.transferSafeguards(),
                req.contractDocumentUrl(),
                req.signedAt(), req.effectiveFrom(), req.expirationDate(),
                req.securityMeasures(), req.breachNotificationCommitmentHours(),
                req.auditRights(), req.auditRightsNotes(),
                req.dataReturnOrDeletionTerms()));
    }

    @PostMapping("/{id}/activate")
    public ProcessorAgreementDto.View activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/terminate")
    public ProcessorAgreementDto.View terminate(@PathVariable UUID id,
                                                @Valid @RequestBody ProcessorAgreementWebDto.TerminateRequest req) {
        return service.terminate(id, new ProcessorAgreementDto.TerminateRequest(req.reason()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }

    @PostMapping("/expire-due")
    public Map<String, Integer> expireDue(
            @RequestParam(defaultValue = "200") @Min(1) @Max(500) int limit) {
        return Map.of("expired", service.expireDue(limit));
    }
}
