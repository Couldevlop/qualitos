package com.openlab.qualitos.core.billing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Contrôleur d'administration du profil de facturation.
 *
 * <p>Réservé au SEUL {@code SUPER_ADMIN} (l'éditeur) : la facturation n'est
 * pas une affaire de tenant. Un administrateur de tenant qui pourrait éditer
 * son propre profil pourrait s'exempter lui-même de facturation — d'où le
 * {@code @PreAuthorize} de classe, comme dans {@link
 * com.openlab.qualitos.core.tenant.TenantController}, qu'aucune méthode ne
 * doit affaiblir.
 *
 * <p>§18.2 règle 2 : le client facturé est désigné par le CHEMIN
 * ({@code @PathVariable tenantId}), jamais par le corps de la requête.
 * {@link BillingProfileDto.SaveCommand} ne porte d'ailleurs aucun champ
 * {@code tenantId} — un éventuel champ du même nom envoyé dans le JSON est
 * silencieusement ignoré par Jackson (configuration Spring Boot par défaut),
 * ce qui rend l'usurpation d'identité client impossible par construction,
 * pas seulement filtrée par une validation qu'on pourrait oublier ailleurs.
 */
@RestController
@RequestMapping("/api/v1/admin/clients/{tenantId}/billing-profile")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Billing Profile", description = "Client billing profile management — Super Admin only")
public class BillingProfileController {

    private final BillingProfileService billingProfileService;

    public BillingProfileController(BillingProfileService billingProfileService) {
        this.billingProfileService = billingProfileService;
    }

    @GetMapping
    @Operation(summary = "Get the billing profile of a client designated by path")
    public ResponseEntity<BillingProfileDto.View> getBillingProfile(@PathVariable UUID tenantId) {
        // L'absence de profil n'est pas une erreur serveur : c'est un client
        // qui n'a pas encore été renseigné (voir BillingProfileService, règle
        // 4 — l'absence de profil n'exempte de rien).
        return billingProfileService.find(tenantId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    @Operation(summary = "Create or update the billing profile of a client designated by path")
    public ResponseEntity<BillingProfileDto.View> saveBillingProfile(
            @PathVariable UUID tenantId,
            @Valid @RequestBody BillingProfileDto.SaveCommand command) {
        return ResponseEntity.ok(billingProfileService.upsert(tenantId, command));
    }
}
