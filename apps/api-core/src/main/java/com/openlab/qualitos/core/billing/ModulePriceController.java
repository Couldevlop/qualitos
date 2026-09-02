package com.openlab.qualitos.core.billing;

import com.openlab.qualitos.core.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur d'administration du tarif des modules.
 *
 * <p>Réservé au SEUL {@code SUPER_ADMIN} (l'éditeur), comme
 * {@link BillingProfileController} : fixer un tarif est une décision de
 * l'éditeur de la plateforme, pas un réglage de tenant. Un administrateur de
 * tenant qui pourrait fixer ses propres tarifs pourrait se les baisser
 * lui-même.
 *
 * <p><b>L'acteur vient du jeton, jamais du corps.</b> Résolu par
 * {@link CurrentUser#requireUserId()}, qui pour un jeton JWT Keycloak
 * ({@code JwtAuthenticationToken}) lit le claim {@code sub} — l'identifiant,
 * stable et non falsifiable côté client, de l'utilisateur authentifié.
 * Ajouter un champ {@code updatedBy} à {@link ModulePriceDto.SaveCommand}
 * aurait permis à n'importe quel SUPER_ADMIN de signer un changement de tarif
 * au nom d'un autre : même principe que le refus, dans
 * {@link BillingProfileController}, de lire le client facturé depuis le
 * corps plutôt que depuis le chemin.
 *
 * <p>{@code CurrentUser.requireUserId()} plutôt qu'un
 * {@code UUID.fromString(authentication.getName())} posé ici : un {@code sub}
 * qui n'est pas un UUID (jeton de compte de service, principal non-JWT,
 * claim personnalisé) lève une {@link com.openlab.qualitos.core.common.UnresolvableActorException}
 * dédiée, traduite en 401 par {@code GlobalExceptionHandler} — pas un 500
 * générique sur une action d'administration de facturation.
 */
@RestController
@RequestMapping("/api/v1/admin/module-prices")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Module Prices", description = "Module pricing catalog — Super Admin only")
public class ModulePriceController {

    private final ModulePriceService modulePriceService;

    public ModulePriceController(ModulePriceService modulePriceService) {
        this.modulePriceService = modulePriceService;
    }

    @GetMapping
    @Operation(summary = "List the module pricing catalog (all modules, tiers and periods)")
    public ResponseEntity<List<ModulePriceDto.View>> listPrices() {
        return ResponseEntity.ok(modulePriceService.findAll());
    }

    @PutMapping
    @Operation(summary = "Set (create or update) the price of a module for a tier and a billing period")
    public ResponseEntity<ModulePriceDto.View> setPrice(
            @Valid @RequestBody ModulePriceDto.SaveCommand command) {
        UUID actor = CurrentUser.requireUserId();
        return ResponseEntity.ok(modulePriceService.setPrice(command, actor));
    }
}
