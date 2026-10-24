package com.openlab.qualitos.quality.dmaic.resolution.web;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.config.RequiresModule;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionDto;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Le paramétrage de la fiche de résolution : questionnaire, types de
 * défaillance, sévérités.
 *
 * <p><b>Le lire est ouvert à tout authentifié</b> : la fiche en a besoin pour
 * se mettre en forme. <b>Le changer est réservé à l'administrateur du client</b>
 * ({@code authz.manage}), comme tout paramétrage.
 */
@RestController
@RequestMapping("/api/v1/dmaic/resolution-settings")
@PreAuthorize("isAuthenticated()")
@RequiresModule("dmaic")
@Tag(name = "DMAIC resolution settings", description = "Tenant questionnaire, failure types and severity scale")
public class ResolutionSettingsController {

    private final ResolutionService service;

    public ResolutionSettingsController(ResolutionService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "The tenant settings, or the platform defaults when none is set")
    public ResolutionDto.SettingsView get() {
        return service.settings();
    }

    @PutMapping
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional
    @Operation(summary = "Replace the settings; answers to removed questions are kept, not shown")
    public ResolutionDto.SettingsView replace(@Valid @RequestBody ResolutionWebDto.SettingsRequest r) {
        return service.replaceSettings(
                r.questions().stream().map(ResolutionWebDto.QuestionRequest::toDomain).toList(),
                r.failureTypes().stream().map(ResolutionWebDto.FailureTypeRequest::toDomain).toList(),
                r.severities().stream().map(ResolutionWebDto.SeverityRequest::toDomain).toList());
    }

    @PostMapping("/reset")
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional
    @Operation(summary = "Go back to the platform defaults")
    public ResolutionDto.SettingsView reset() {
        return service.resetSettings();
    }
}
