package com.openlab.qualitos.quality.aieudb.web;

import com.openlab.qualitos.quality.aieudb.application.EudbRegistrationDto;
import com.openlab.qualitos.quality.aieudb.application.EudbRegistrationService;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStatus;
import jakarta.validation.Valid;
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
 * Endpoints — enregistrements EUDB AI Act (Art. 49 / 71).
 */
@RestController
@RequestMapping("/api/v1/ai-act/eudb")
@Validated
public class EudbRegistrationController {

    private final EudbRegistrationService service;

    public EudbRegistrationController(EudbRegistrationService service) {
        this.service = service;
    }

    @GetMapping
    public List<EudbRegistrationDto.View> list(
            @RequestParam(required = false) EudbRegistrationStatus status) {
        return service.list(status);
    }

    @GetMapping("/by-system")
    public List<EudbRegistrationDto.View> byAiSystem(@RequestParam @NotNull UUID aiSystemId) {
        return service.listByAiSystem(aiSystemId);
    }

    @GetMapping("/{id}")
    public EudbRegistrationDto.View get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/by-reference")
    public EudbRegistrationDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @GetMapping("/by-eudb-id")
    public EudbRegistrationDto.View getByEudbId(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^EUDB-AI-[A-Z0-9]{6,32}$") String eudbId) {
        return service.getByEudbId(eudbId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EudbRegistrationDto.View draft(
            @Valid @RequestBody EudbRegistrationWebDto.DraftRequest req) {
        return service.draft(new EudbRegistrationDto.DraftRequest(
                req.reference(), req.aiSystemId(),
                req.providerEntityName(), req.providerEuRepresentative(),
                req.memberStateOfReference(), req.intendedPurposeSummary(),
                req.technicalDocumentationReference(), req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public EudbRegistrationDto.View edit(@PathVariable UUID id,
            @Valid @RequestBody EudbRegistrationWebDto.EditRequest req) {
        return service.edit(id, new EudbRegistrationDto.EditRequest(
                req.providerEntityName(), req.providerEuRepresentative(),
                req.memberStateOfReference(), req.intendedPurposeSummary(),
                req.technicalDocumentationReference()));
    }

    @PostMapping("/{id}/submit")
    public EudbRegistrationDto.View submit(@PathVariable UUID id,
            @Valid @RequestBody EudbRegistrationWebDto.SubmitRequest req) {
        return service.submit(id, new EudbRegistrationDto.SubmitRequest(req.submittedByUserId()));
    }

    @PostMapping("/{id}/mark-registered")
    public EudbRegistrationDto.View markRegistered(@PathVariable UUID id,
            @Valid @RequestBody EudbRegistrationWebDto.MarkRegisteredRequest req) {
        return service.markRegistered(id, new EudbRegistrationDto.MarkRegisteredRequest(
                req.eudbId(), req.registrationDate()));
    }

    @PostMapping("/{id}/declare-update")
    public EudbRegistrationDto.View declareUpdate(@PathVariable UUID id,
            @Valid @RequestBody EudbRegistrationWebDto.DeclareUpdateRequest req) {
        return service.declareUpdate(id, new EudbRegistrationDto.DeclareUpdateRequest(
                req.updateSummary(), req.updateDate()));
    }

    @PostMapping("/{id}/reject")
    public EudbRegistrationDto.View reject(@PathVariable UUID id,
            @Valid @RequestBody EudbRegistrationWebDto.RejectRequest req) {
        return service.reject(id, new EudbRegistrationDto.RejectRequest(req.reason()));
    }

    @PostMapping("/{id}/retire")
    public EudbRegistrationDto.View retire(@PathVariable UUID id,
            @Valid @RequestBody EudbRegistrationWebDto.RetireRequest req) {
        return service.retire(id, new EudbRegistrationDto.RetireRequest(req.reason()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
