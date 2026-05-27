package com.openlab.qualitos.quality.capa;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/capa")
public class CapaController {

    private final CapaService service;

    public CapaController(CapaService service) {
        this.service = service;
    }

    @GetMapping("/cases")
    public Page<CapaDto.CaseResponse> list(
            @RequestParam(required = false) CapaStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(status, pageable);
    }

    @PostMapping("/cases")
    @ResponseStatus(HttpStatus.CREATED)
    public CapaDto.CaseResponse create(@Valid @RequestBody CapaDto.CreateCaseRequest request) {
        return service.createCase(request);
    }

    @GetMapping("/cases/{id}")
    public CapaDto.CaseResponse get(@PathVariable UUID id) { return service.findById(id); }

    @PatchMapping("/cases/{id}")
    public CapaDto.CaseResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody CapaDto.UpdateCaseRequest request) {
        return service.updateCase(id, request);
    }

    @PatchMapping("/cases/{id}/start")
    public CapaDto.CaseResponse start(@PathVariable UUID id) { return service.startCase(id); }

    @PatchMapping("/cases/{id}/resolve")
    public CapaDto.CaseResponse resolve(@PathVariable UUID id) { return service.resolveCase(id); }

    @PatchMapping("/cases/{id}/effectiveness")
    public CapaDto.CaseResponse verifyEffectiveness(
            @PathVariable UUID id,
            @Valid @RequestBody CapaDto.EffectivenessRequest request) {
        return service.verifyEffectiveness(id, request);
    }

    @PatchMapping("/cases/{id}/reject")
    public CapaDto.CaseResponse reject(@PathVariable UUID id) { return service.rejectCase(id); }

    @DeleteMapping("/cases/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteCase(id); }

    @PostMapping("/cases/{id}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public CapaDto.ActionResponse addAction(@PathVariable UUID id,
                                            @Valid @RequestBody CapaDto.ActionRequest request) {
        return service.addAction(id, request);
    }

    // Suggestion d'actions par l'IA (§4.2) — POST : appel LLM, pas de persistance.
    @PostMapping("/cases/{id}/suggest-actions")
    public java.util.List<CapaDto.SuggestedAction> suggestActions(@PathVariable UUID id) {
        return service.suggestActions(id);
    }

    @PatchMapping("/cases/{id}/actions/{actionId}")
    public CapaDto.ActionResponse updateAction(
            @PathVariable UUID id,
            @PathVariable UUID actionId,
            @RequestBody CapaDto.ActionRequest request) {
        return service.updateAction(id, actionId, request);
    }

    @DeleteMapping("/cases/{id}/actions/{actionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAction(@PathVariable UUID id, @PathVariable UUID actionId) {
        service.deleteAction(id, actionId);
    }
}
