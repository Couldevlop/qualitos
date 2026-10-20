package com.openlab.qualitos.quality.spc.capability.web;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import com.openlab.qualitos.quality.spc.capability.application.SpcStudyDto;
import com.openlab.qualitos.quality.spc.capability.application.SpcStudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * « Paramètres SPC » : les seuils de Cpk du client (maîtrise, surveillance).
 *
 * <p><b>Les lire est ouvert à tout authentifié</b> : l'écran d'une étude
 * neuve en a besoin pour colorer le Cpk pendant la saisie. <b>Les changer est
 * réservé à l'administrateur du client</b> ({@code authz.manage}), comme les
 * autres paramétrages.
 */
@RestController
@RequestMapping("/api/v1/spc/settings")
@PreAuthorize("isAuthenticated()")
@Tag(name = "SPC settings", description = "Tenant Cpk thresholds")
public class SpcSettingsController {

    private final SpcStudyService service;

    public SpcSettingsController(SpcStudyService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "The Cpk thresholds in force: the tenant's, or the platform defaults")
    public SpcStudyDto.SettingsView get() {
        return service.settings();
    }

    @PutMapping
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    @Transactional
    @Operation(summary = "Set the Cpk thresholds; recorded studies keep the thresholds they were saved with")
    public SpcStudyDto.SettingsView replace(@Valid @RequestBody SpcStudyWebDto.SettingsRequest requete) {
        return service.updateSettings(new SpcStudyDto.SettingsCommand(
                requete.capableThreshold(), requete.watchThreshold()));
    }
}
