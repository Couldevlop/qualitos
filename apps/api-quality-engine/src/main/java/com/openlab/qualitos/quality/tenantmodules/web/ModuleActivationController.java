package com.openlab.qualitos.quality.tenantmodules.web;

import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationDto;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant-modules")
@Validated
public class ModuleActivationController {

    private final ModuleActivationService service;

    public ModuleActivationController(ModuleActivationService service) { this.service = service; }

    @GetMapping("/catalog")
    public List<ModuleActivationDto.CatalogEntryView> catalog() { return service.listCatalog(); }

    @GetMapping("/activations")
    public List<ModuleActivationDto.ActivationView> mine() { return service.listForCurrentTenant(); }

    @GetMapping("/activations/{id}")
    public ModuleActivationDto.ActivationView get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping("/summary")
    public ModuleActivationDto.TenantModuleSummary summary() { return service.summary(); }

    @GetMapping("/enabled/{moduleCode}")
    public java.util.Map<String, Boolean> isEnabled(@PathVariable String moduleCode) {
        return java.util.Map.of("enabled", service.isEnabled(moduleCode));
    }

    @PostMapping("/activations/trial")
    @ResponseStatus(HttpStatus.CREATED)
    public ModuleActivationDto.ActivationView startTrial(
            @Valid @RequestBody ModuleActivationWebDto.StartTrialRequest req) {
        return service.startTrial(new ModuleActivationDto.StartTrialRequest(
                req.moduleCode(), req.trialEndsAt(), req.actor()));
    }

    @PostMapping("/activations")
    @ResponseStatus(HttpStatus.CREATED)
    public ModuleActivationDto.ActivationView activate(
            @Valid @RequestBody ModuleActivationWebDto.ActivateRequest req) {
        return service.activate(new ModuleActivationDto.ActivateRequest(
                req.moduleCode(), req.expiresAt(), req.actor()));
    }

    @PostMapping("/activations/{id}/convert")
    public ModuleActivationDto.ActivationView convert(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleActivationWebDto.ConvertTrialRequest req) {
        return service.convertTrial(id,
                new ModuleActivationDto.ConvertTrialRequest(req.expiresAt(), req.actor()));
    }

    @PostMapping("/activations/{id}/suspend")
    public ModuleActivationDto.ActivationView suspend(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleActivationWebDto.SuspendRequest req) {
        return service.suspend(id, new ModuleActivationDto.SuspendRequest(req.actor()));
    }

    @PostMapping("/activations/{id}/resume")
    public ModuleActivationDto.ActivationView resume(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleActivationWebDto.ResumeRequest req) {
        return service.resume(id, new ModuleActivationDto.ResumeRequest(req.actor()));
    }

    @PostMapping("/activations/{id}/disable")
    public ModuleActivationDto.ActivationView disable(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleActivationWebDto.DisableRequest req) {
        return service.disable(id, new ModuleActivationDto.DisableRequest(req.actor()));
    }

    @PostMapping("/activations/{id}/expire")
    public ModuleActivationDto.ActivationView expire(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleActivationWebDto.ExpireRequest req) {
        return service.expire(id, new ModuleActivationDto.ExpireRequest(req.actor()));
    }

    @PostMapping("/activations/{id}/tier")
    public ModuleActivationDto.ActivationView changeTier(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleActivationWebDto.ChangeTierRequest req) {
        return service.changeTier(id,
                new ModuleActivationDto.ChangeTierRequest(req.newTier(), req.actor()));
    }

    @PostMapping("/activations/{id}/configure")
    public ModuleActivationDto.ActivationView configure(
            @PathVariable UUID id,
            @Valid @RequestBody ModuleActivationWebDto.ConfigureRequest req) {
        return service.configure(id,
                new ModuleActivationDto.ConfigureRequest(req.configurationJson(), req.actor()));
    }

    /** Scheduler-callable : expire en masse. Admin-only en pratique (sécurité côté gateway). */
    @PostMapping("/expire-due")
    public java.util.Map<String, Integer> expireDue(
            @RequestParam(defaultValue = "200") @Min(1) @Max(500) int limit) {
        return java.util.Map.of("expired", service.expireDue(limit));
    }
}
