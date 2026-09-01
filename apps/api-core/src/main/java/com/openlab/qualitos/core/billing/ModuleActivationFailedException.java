package com.openlab.qualitos.core.billing;

/**
 * Le moteur de qualité n'a pas pu appliquer une décision commerciale : le
 * module n'a pas été ouvert (ou fermé) pour le client visé.
 *
 * <p>Elle existe pour que {@link SubscriptionService} puisse ABANDONNER la
 * souscription plutôt que l'enregistrer quand même. Sans elle, une panne du
 * moteur produirait un abonnement facturé pour un module que le client ne peut
 * pas utiliser — et personne ne s'en apercevrait avant la réclamation.
 *
 * <p>Traduite en {@code 502 Bad Gateway} par {@code GlobalExceptionHandler} :
 * ce n'est pas la faute de l'appelant (400), ni une panne d'{@code api-core}
 * (500), mais un service en aval qui n'a pas répondu ce qu'il fallait.
 */
public class ModuleActivationFailedException extends RuntimeException {

    public ModuleActivationFailedException(String message) {
        super(message);
    }

    public ModuleActivationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
