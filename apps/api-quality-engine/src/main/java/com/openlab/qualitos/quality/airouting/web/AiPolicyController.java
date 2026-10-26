package com.openlab.qualitos.quality.airouting.web;

import com.openlab.qualitos.quality.airouting.application.AiPolicyAdminService;
import com.openlab.qualitos.quality.airouting.application.AiPolicyDto;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.authz.web.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * « Fournisseur d'IA » : à qui partent les appels LLM du tenant (ADR 0102, §12.2).
 *
 * <p>Paramétrage du client : lire comme écrire est réservé à son administrateur
 * ({@code authz.manage}), comme les autres paramétrages. Le tenant vient du jeton.
 */
@RestController
@RequestMapping("/api/v1/ai/policy")
@PreAuthorize("isAuthenticated()")
@RequiresPermission(Permission.AUTHZ_MANAGE)
@Tag(name = "AI policy", description = "Tenant LLM provider routing")
public class AiPolicyController {

    private final AiPolicyAdminService service;

    public AiPolicyController(AiPolicyAdminService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "The provider receiving the tenant's LLM calls, and the available choices")
    public AiPolicyDto.View get() {
        return service.view();
    }

    @PutMapping
    @Transactional
    @Operation(summary = "Pin the tenant's LLM calls on a provider")
    public AiPolicyDto.View pin(@Valid @RequestBody AiPolicyWebDto request) {
        return service.pin(new AiPolicyDto.PinCommand(request.provider()));
    }

    @DeleteMapping
    @Transactional
    @Operation(summary = "Return the tenant to the platform default provider")
    public AiPolicyDto.View release() {
        return service.release();
    }
}
