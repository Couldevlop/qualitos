package com.openlab.qualitos.core.partner;

import com.openlab.qualitos.core.common.CurrentUser;
import com.openlab.qualitos.core.common.MissingTenantContextException;
import com.openlab.qualitos.core.security.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Les accords, vus par le client : qui l'éditeur lui propose, et ce qu'il
 * décide.
 *
 * <p><b>C'est ici que se joue la règle.</b> Un cabinet n'entre chez un client
 * que si ce client a accordé — pas si l'éditeur l'a inscrit à un portefeuille.
 * La décision est donc portée par une route du client, sous son rôle
 * d'administrateur, et le client vient du jeton, jamais du corps ni du chemin
 * ({@code CLAUDE.md} §18.2.2).
 *
 * <p>Pas de {@code @SaasOnly} : un client on-premise peut lui aussi faire
 * intervenir un cabinet — c'est même chez lui que l'accord compte le plus,
 * puisqu'il n'y a pas d'éditeur entre les deux.
 */
@RestController
@RequestMapping("/api/v1/partner-access")
@PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_TENANT')")
@Tag(name = "Partner access", description = "Les accords d'accès d'un client avec des cabinets")
public class PartnerAccessController {

    private final PartnerAccessService service;

    public PartnerAccessController(PartnerAccessService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "The engagements that concern this tenant")
    public ResponseEntity<List<PartnerDto.EngagementResponse>> list() {
        return ResponseEntity.ok(service.listForTenant(currentTenant()));
    }

    @PostMapping("/{engagementId}/grant")
    @Operation(summary = "Grant a firm access to this tenant, bounded and revocable")
    public ResponseEntity<PartnerDto.EngagementResponse> grant(
            @PathVariable UUID engagementId, @Valid @RequestBody PartnerDto.GrantRequest request) {
        return ResponseEntity.ok(service.grant(currentTenant(), engagementId, request, actor()));
    }

    @PostMapping("/{engagementId}/decline")
    @Operation(summary = "Decline a proposed engagement")
    public ResponseEntity<PartnerDto.EngagementResponse> decline(@PathVariable UUID engagementId) {
        return ResponseEntity.ok(service.decline(currentTenant(), engagementId, actor()));
    }

    @PostMapping("/{engagementId}/revoke")
    @Operation(summary = "Take back a granted access, at once")
    public ResponseEntity<PartnerDto.EngagementResponse> revoke(@PathVariable UUID engagementId) {
        return ResponseEntity.ok(service.revoke(currentTenant(), engagementId, actor()));
    }

    private UUID currentTenant() {
        String raw = TenantContext.getTenantId();
        if (raw == null || raw.isBlank()) {
            throw new MissingTenantContextException();
        }
        return UUID.fromString(raw);
    }

    /** L'acteur vient de l'identité authentifiée, jamais du corps (OWASP A01). */
    private UUID actor() {
        return CurrentUser.userId().orElse(null);
    }
}
