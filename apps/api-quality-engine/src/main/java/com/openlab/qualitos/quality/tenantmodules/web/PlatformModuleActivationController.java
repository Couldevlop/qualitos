package com.openlab.qualitos.quality.tenantmodules.web;

import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationDto;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Surface <b>plateforme</b> : l'éditeur ouvre et ferme des modules POUR un
 * client désigné.
 *
 * <p>Elle existe parce que {@link ModuleActivationController} agit sur le
 * tenant du CONTEXTE, ce qui est juste quand un utilisateur agit chez lui et
 * inutilisable quand l'éditeur applique une décision commerciale prise dans
 * {@code api-core} : le client concerné n'est pas le sien. Le client est donc
 * désigné par le CHEMIN.
 *
 * <p><b>Ce n'est pas une entorse à §18.2 règle 2.</b> La règle empêche un
 * utilisateur de tenant de forger son appartenance ; ici l'acteur est
 * l'éditeur, sur une surface réservée au seul {@code SUPER_ADMIN} — verrouillée
 * deux fois, par ce {@code @PreAuthorize} de classe et par une règle d'URL dans
 * {@code SecurityConfig}. Un administrateur de tenant qui atteindrait cette
 * surface pourrait s'ouvrir des modules chez un concurrent.
 *
 * <p><b>Idempotence.</b> Ouvrir un module déjà ouvert répond 201 sans rien
 * changer ; fermer un module déjà fermé répond 204. C'est délibéré :
 * l'appelant est {@code api-core}, qui exprime un ÉTAT VOULU (« ce client doit
 * disposer de ce module ») et non une transition à exécuter. Refuser en 409
 * comme le fait la surface ordinaire rendrait impossible de souscrire un
 * module qu'un administrateur aurait activé à la main — le même piège que le
 * « Missing dependency » définitif corrigé par la PR #122.
 *
 * <p>Chaque acte est inscrit au journal chaîné du client, sous
 * {@code tenant.module.platform_activated} / {@code platform_deactivated}, avec
 * l'acteur lu du jeton (§18.2 règle 5). L'action est nommée à part parce que
 * l'acteur d'une activation plateforme n'appartient pas au client : un auditeur
 * doit pouvoir le lire, pas le déduire.
 */
@RestController
@RequestMapping("/api/v1/platform/tenants/{tenantId}/modules")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Platform Modules", description = "Module activation for a designated client — Super Admin only")
public class PlatformModuleActivationController {

    private final ModuleActivationService service;

    public PlatformModuleActivationController(ModuleActivationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List the module activations of a designated client")
    public List<ModuleActivationDto.ActivationView> list(@PathVariable UUID tenantId) {
        return service.listFor(tenantId);
    }

    @PostMapping("/{moduleCode}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a module for a designated client (idempotent)")
    public ModuleActivationDto.ActivationView activate(@PathVariable UUID tenantId,
                                                       @PathVariable String moduleCode) {
        // Aucune date d'expiration : l'échéance est une affaire COMMERCIALE, et
        // elle vit dans l'abonnement d'api-core (colonne next_renewal). La poser
        // ici aussi créerait deux dates concurrentes pour un seul contrat, et
        // c'est celle que personne ne regarde qui finirait par couper le module.
        return service.activateFor(tenantId, moduleCode, null);
    }

    @DeleteMapping("/{moduleCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Close a module for a designated client (idempotent)")
    public void deactivate(@PathVariable UUID tenantId, @PathVariable String moduleCode) {
        service.deactivateFor(tenantId, moduleCode);
    }
}
