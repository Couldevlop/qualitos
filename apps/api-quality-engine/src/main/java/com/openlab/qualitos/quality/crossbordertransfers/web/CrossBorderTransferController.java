package com.openlab.qualitos.quality.crossbordertransfers.web;

import com.openlab.qualitos.quality.crossbordertransfers.application.CrossBorderTransferDto;
import com.openlab.qualitos.quality.crossbordertransfers.application.CrossBorderTransferService;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStatus;
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
 * Endpoints — registre des transferts internationaux (RGPD Chapitre V).
 */
@RestController
@RequestMapping("/api/v1/gdpr/cross-border-transfers")
@Validated
public class CrossBorderTransferController {

    private final CrossBorderTransferService service;

    public CrossBorderTransferController(CrossBorderTransferService service) {
        this.service = service;
    }

    @GetMapping
    public List<CrossBorderTransferDto.View> list(
            @RequestParam(required = false) CrossBorderTransferStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public CrossBorderTransferDto.View get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/by-reference")
    public CrossBorderTransferDto.View getByReference(
            @RequestParam @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$") String reference) {
        return service.getByReference(reference);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CrossBorderTransferDto.View create(
            @Valid @RequestBody CrossBorderTransferWebDto.CreateRequest req) {
        return service.create(new CrossBorderTransferDto.CreateRequest(
                req.reference(), req.recipientName(), req.recipientLegalEntity(),
                req.recipientContact(), req.destinationCountries(), req.mechanism(),
                req.safeguardsDescription(), req.safeguardsDocumentUrl(),
                req.derogationJustification(), req.dataCategories(),
                req.linkedProcessingActivityIds(), req.linkedProcessorAgreementIds(),
                req.createdByUserId()));
    }

    @PutMapping("/{id}")
    public CrossBorderTransferDto.View edit(@PathVariable UUID id,
                                            @Valid @RequestBody CrossBorderTransferWebDto.EditRequest req) {
        return service.edit(id, new CrossBorderTransferDto.EditRequest(
                req.recipientName(), req.recipientLegalEntity(), req.recipientContact(),
                req.destinationCountries(), req.mechanism(),
                req.safeguardsDescription(), req.safeguardsDocumentUrl(),
                req.derogationJustification(), req.dataCategories(),
                req.linkedProcessingActivityIds(), req.linkedProcessorAgreementIds()));
    }

    @PostMapping("/{id}/activate")
    public CrossBorderTransferDto.View activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/suspend")
    public CrossBorderTransferDto.View suspend(@PathVariable UUID id,
                                               @Valid @RequestBody CrossBorderTransferWebDto.SuspendRequest req) {
        return service.suspend(id, new CrossBorderTransferDto.SuspendRequest(req.reason()));
    }

    @PostMapping("/{id}/terminate")
    public CrossBorderTransferDto.View terminate(@PathVariable UUID id,
                                                 @Valid @RequestBody CrossBorderTransferWebDto.TerminateRequest req) {
        return service.terminate(id, new CrossBorderTransferDto.TerminateRequest(req.reason()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
