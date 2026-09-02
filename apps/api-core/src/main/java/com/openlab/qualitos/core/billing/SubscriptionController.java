package com.openlab.qualitos.core.billing;

import com.openlab.qualitos.core.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Contrôleur d'administration des abonnements d'un client.
 *
 * <p>Réservé au SEUL {@code SUPER_ADMIN} (l'éditeur), comme
 * {@link BillingProfileController} et {@link ModulePriceController} : souscrire
 * un module engage une facturation. Un administrateur de tenant qui pourrait
 * souscrire pour lui-même pourrait aussi bien résilier la veille de
 * l'échéance.
 *
 * <p>§18.2 règle 2 : le client est désigné par le CHEMIN.
 * {@link SubscriptionDto.SubscribeCommand} ne porte aucun champ
 * {@code tenantId} — l'usurpation est impossible par construction, pas
 * seulement filtrée par une validation qu'on pourrait oublier ailleurs.
 *
 * <p>§18.2 règle 5 : l'acteur vient du jeton
 * ({@link CurrentUser#requireUserId()}), jamais du corps. Souscrire et
 * résilier sont des actes commerciaux ; ils doivent rester attribuables des
 * années plus tard, quand plus personne ne se souviendra de qui a signé.
 *
 * <p>La résiliation est adressée par l'identifiant de l'ABONNEMENT et non par
 * le code du module : un client peut avoir souscrit puis résilié puis
 * re-souscrit le même module, et seul l'identifiant distingue lequel des
 * contrats on ferme.
 */
@RestController
@RequestMapping("/api/v1/admin/clients/{tenantId}/subscriptions")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Subscriptions", description = "Client module subscriptions — Super Admin only")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    @Operation(summary = "List the live subscriptions of a client designated by path")
    public ResponseEntity<List<SubscriptionDto.View>> listSubscriptions(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(subscriptionService.activeFor(tenantId));
    }

    @PostMapping
    @Operation(summary = "Subscribe a module for a client, at a tier and a billing period")
    public ResponseEntity<SubscriptionDto.View> subscribe(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SubscriptionDto.SubscribeCommand command) {
        UUID actor = CurrentUser.requireUserId();
        // 201 : la souscription crée une ressource — l'abonnement — que le
        // client de l'API pourra ensuite adresser par son identifiant.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(tenantId, command, actor));
    }

    @DeleteMapping("/{subscriptionId}")
    @Operation(summary = "Cancel a subscription and close the corresponding module")
    public ResponseEntity<SubscriptionDto.View> cancel(
            @PathVariable UUID tenantId,
            @PathVariable UUID subscriptionId) {
        UUID actor = CurrentUser.requireUserId();
        // 200 avec le corps, et non 204 : la résiliation RÉPOND quelque chose
        // — la date à laquelle elle a pris effet, qui décide de la dernière
        // période facturée. Un 204 obligerait à relire pour l'apprendre.
        return ResponseEntity.ok(subscriptionService.cancel(tenantId, subscriptionId, actor));
    }
}
