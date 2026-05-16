package com.openlab.qualitos.quality.ropa.web;

import com.openlab.qualitos.quality.ropa.application.ProcessingActivityDto;
import com.openlab.qualitos.quality.ropa.application.ProcessingActivityService;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStatus;
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
 * Endpoints — registre des traitements (RoPA, RGPD Art. 30).
 */
@RestController
@RequestMapping("/api/v1/gdpr/processing-activities")
@Validated
public class ProcessingActivityController {

    private final ProcessingActivityService service;

    public ProcessingActivityController(ProcessingActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProcessingActivityDto.View> list(
            @RequestParam(required = false) ProcessingActivityStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public ProcessingActivityDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/by-reference")
    public ProcessingActivityDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProcessingActivityDto.View create(
            @Valid @RequestBody ProcessingActivityWebDto.CreateRequest req) {
        return service.create(new ProcessingActivityDto.CreateRequest(
                req.reference(), req.name(), req.purposes(),
                req.lawfulBasis(), req.lawfulBasisDetails(),
                req.controllerName(), req.controllerContact(), req.dpoContact(),
                req.jointControllerName(), req.jointControllerContact(),
                req.dataSubjectCategories(), req.dataCategories(),
                req.specialCategoriesProcessed(), req.specialCategoriesJustification(),
                req.recipientCategories(), req.thirdCountryTransfers(),
                req.transferSafeguards(), req.linkedRetentionRuleIds(),
                req.technicalMeasures(), req.organizationalMeasures(),
                req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public ProcessingActivityDto.View edit(
            @PathVariable UUID id,
            @Valid @RequestBody ProcessingActivityWebDto.EditRequest req) {
        return service.edit(id, new ProcessingActivityDto.EditRequest(
                req.name(), req.purposes(),
                req.lawfulBasis(), req.lawfulBasisDetails(),
                req.controllerName(), req.controllerContact(), req.dpoContact(),
                req.jointControllerName(), req.jointControllerContact(),
                req.dataSubjectCategories(), req.dataCategories(),
                req.specialCategoriesProcessed(), req.specialCategoriesJustification(),
                req.recipientCategories(), req.thirdCountryTransfers(),
                req.transferSafeguards(), req.linkedRetentionRuleIds(),
                req.technicalMeasures(), req.organizationalMeasures()));
    }

    @PostMapping("/{id}/activate")
    public ProcessingActivityDto.View activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/archive")
    public ProcessingActivityDto.View archive(@PathVariable UUID id) {
        return service.archive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
