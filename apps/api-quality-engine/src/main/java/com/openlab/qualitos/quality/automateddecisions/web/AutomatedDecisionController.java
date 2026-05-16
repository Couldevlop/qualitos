package com.openlab.qualitos.quality.automateddecisions.web;

import com.openlab.qualitos.quality.automateddecisions.application.AutomatedDecisionDto;
import com.openlab.qualitos.quality.automateddecisions.application.AutomatedDecisionService;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints — registre des décisions automatisées (RGPD Art. 22).
 */
@RestController
@RequestMapping("/api/v1/gdpr/automated-decisions")
@Validated
public class AutomatedDecisionController {

    private final AutomatedDecisionService service;

    public AutomatedDecisionController(AutomatedDecisionService service) {
        this.service = service;
    }

    @GetMapping
    public List<AutomatedDecisionDto.View> list(
            @RequestParam(required = false) AutomatedDecisionStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public AutomatedDecisionDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/by-reference")
    public AutomatedDecisionDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AutomatedDecisionDto.View create(
            @Valid @RequestBody AutomatedDecisionWebDto.CreateRequest req) {
        return service.create(new AutomatedDecisionDto.CreateRequest(
                req.reference(), req.name(), req.description(),
                req.decisionType(), req.art22LawfulBasis(), req.lawfulBasisDetails(),
                req.inputDataCategories(), req.linkedProcessingActivityIds(),
                req.linkedDpiaId(),
                req.algorithmDescription(), req.significanceForSubject(),
                req.humanReviewMechanism(), req.objectionMechanism(),
                req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public AutomatedDecisionDto.View edit(@PathVariable UUID id,
                                          @Valid @RequestBody AutomatedDecisionWebDto.EditRequest req) {
        return service.edit(id, new AutomatedDecisionDto.EditRequest(
                req.name(), req.description(),
                req.decisionType(), req.art22LawfulBasis(), req.lawfulBasisDetails(),
                req.inputDataCategories(), req.linkedProcessingActivityIds(),
                req.linkedDpiaId(),
                req.algorithmDescription(), req.significanceForSubject(),
                req.humanReviewMechanism(), req.objectionMechanism()));
    }

    @PostMapping("/{id}/activate")
    public AutomatedDecisionDto.View activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/deprecate")
    public AutomatedDecisionDto.View deprecate(@PathVariable UUID id) {
        return service.deprecate(id);
    }

    @PostMapping("/{id}/archive")
    public AutomatedDecisionDto.View archive(@PathVariable UUID id) {
        return service.archive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
