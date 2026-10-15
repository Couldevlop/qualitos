package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
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
    @RequiresPermission(Permission.CAPA_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public CapaDto.CaseResponse create(@Valid @RequestBody CapaDto.CreateCaseRequest request) {
        return service.createCase(request);
    }

    /**
     * Ouvre un dossier avec son action unique, depuis la liste CAPA (ADR 0088).
     *
     * <p>La source est choisie librement, sauf {@code RISK} : un dossier de
     * risque se rattache à sa fiche par la référence R-xxx, et seul le
     * registre peut la poser — sans quoi n'importe qui ferait apparaître un
     * dossier dans le traitement d'un risque qu'il n'a pas ouvert. Depuis une
     * NC, un constat ou un risque, c'est l'écran d'origine qui ouvre le
     * dossier, par sa propre route, et fixe la source.
     */
    @PostMapping("/cases/open")
    @RequiresPermission(Permission.CAPA_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public CapaDto.CaseResponse open(@Valid @RequestBody CapaDto.OpenCaseRequest request) {
        CapaSourceType source = request.sourceType() != null ? request.sourceType() : CapaSourceType.INTERNAL;
        if (source == CapaSourceType.RISK) {
            throw new CapaValidationException("A CAPA on a risk is opened from the risk register");
        }
        return service.openCase(request, source, request.sourceRef());
    }

    @GetMapping("/cases/{id}")
    public CapaDto.CaseResponse get(@PathVariable UUID id) { return service.findById(id); }

    @PatchMapping("/cases/{id}")
    @RequiresPermission(Permission.CAPA_EDIT)
    public CapaDto.CaseResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody CapaDto.UpdateCaseRequest request) {
        return service.updateCase(id, request);
    }

    @PatchMapping("/cases/{id}/start")
    @RequiresPermission(Permission.CAPA_EDIT)
    public CapaDto.CaseResponse start(@PathVariable UUID id) { return service.startCase(id); }

    @PatchMapping("/cases/{id}/resolve")
    @RequiresPermission(Permission.CAPA_RESOLVE)
    public CapaDto.CaseResponse resolve(@PathVariable UUID id) { return service.resolveCase(id); }

    @PatchMapping("/cases/{id}/effectiveness")
    @RequiresPermission(Permission.CAPA_VERIFY)
    public CapaDto.CaseResponse verifyEffectiveness(
            @PathVariable UUID id,
            @Valid @RequestBody CapaDto.EffectivenessRequest request) {
        return service.verifyEffectiveness(id, request);
    }

    @PatchMapping("/cases/{id}/reject")
    @RequiresPermission(Permission.CAPA_REJECT)
    public CapaDto.CaseResponse reject(@PathVariable UUID id) { return service.rejectCase(id); }

    @DeleteMapping("/cases/{id}")
    @RequiresPermission(Permission.CAPA_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.deleteCase(id); }

    @PostMapping("/cases/{id}/actions")
    @RequiresPermission(Permission.CAPA_ACTION_MANAGE)
    @ResponseStatus(HttpStatus.CREATED)
    public CapaDto.ActionResponse addAction(@PathVariable UUID id,
                                            @Valid @RequestBody CapaDto.ActionRequest request) {
        return service.addAction(id, request);
    }

    @PatchMapping("/cases/{id}/actions/{actionId}")
    @RequiresPermission(Permission.CAPA_ACTION_UPDATE)
    public CapaDto.ActionResponse updateAction(
            @PathVariable UUID id,
            @PathVariable UUID actionId,
            @RequestBody CapaDto.ActionRequest request) {
        return service.updateAction(id, actionId, request);
    }

    @DeleteMapping("/cases/{id}/actions/{actionId}")
    @RequiresPermission(Permission.CAPA_ACTION_MANAGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAction(@PathVariable UUID id, @PathVariable UUID actionId) {
        service.deleteAction(id, actionId);
    }
}
