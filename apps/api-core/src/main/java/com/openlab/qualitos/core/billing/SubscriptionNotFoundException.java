package com.openlab.qualitos.core.billing;

import java.util.UUID;

/**
 * L'abonnement demandé n'existe pas. Traduite en 404 par
 * {@code GlobalExceptionHandler} — comme {@code TenantNotFoundException}, et
 * pour la même raison : un identifiant inconnu est une erreur de l'appelant,
 * pas une panne du serveur.
 */
public class SubscriptionNotFoundException extends RuntimeException {

    public SubscriptionNotFoundException(UUID subscriptionId) {
        super("Abonnement introuvable : " + subscriptionId);
    }
}
