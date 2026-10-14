package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * « Paramètres MOC » : seuils, poids, délai de rappel, rôles de validation et
 * titulaires. Réservé à l'administrateur du client ({@code authz.manage}, comme
 * les circuits de validation, ADR 0080) — en lecture comme en écriture.
 */
@RestController
@RequestMapping("/api/v1/changes/settings")
@PreAuthorize("isAuthenticated()")
public class MocSettingsController {

    private final MocSettingsService service;

    public MocSettingsController(MocSettingsService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    public MocDto.SettingsView get() {
        return service.view();
    }

    @PutMapping
    @RequiresPermission(Permission.AUTHZ_MANAGE)
    public MocDto.SettingsView replace(@Valid @RequestBody MocDto.SettingsForm form) {
        return service.replace(form);
    }
}
