package com.openlab.qualitos.core.billing;

import java.util.UUID;

/**
 * Port sortant vers le moteur de qualité : ce qu'{@code api-core} a besoin de
 * lui demander, et rien de plus.
 *
 * <p>Une interface, et non un appel HTTP posé dans
 * {@link SubscriptionService} : la règle métier — « on ne facture pas un module
 * qu'on n'a pas réussi à ouvrir » — se teste alors sans serveur en face, et le
 * jour où le moteur cesserait d'être joignable par REST (message, appel
 * interne), seul l'adaptateur change.
 *
 * <p>Le contrat est volontairement PAUVRE : deux verbes, pas de vue. La
 * facturation n'a pas à savoir ce qu'est un essai, une suspension ou un
 * palier d'activation — ce vocabulaire appartient au moteur, et l'importer ici
 * ferait remonter ses états dans la couche commerciale.
 *
 * <p>Les deux verbes sont IDEMPOTENTS du point de vue de l'appelant : demander
 * l'ouverture d'un module déjà ouvert n'est pas une erreur. C'est
 * {@link SubscriptionService} qui refuse la double souscription, sur sa propre
 * base, avant même d'appeler ici — l'unicité commerciale se décide dans la
 * vérité commerciale, pas dans sa conséquence technique.
 */
public interface ModuleActivationPort {

    /**
     * Ouvre le module pour le client désigné.
     *
     * @throws ModuleActivationFailedException le moteur a refusé ou n'a pas
     *                                         répondu — l'abonnement ne doit
     *                                         alors PAS être enregistré.
     */
    void activate(UUID tenantId, String moduleCode);

    /**
     * Ferme le module pour le client désigné.
     *
     * <p>Fermer, c'est fermer l'ÉCRITURE, jamais la lecture (invariant posé par
     * {@code RequiresModule} côté moteur) : une résiliation ne doit pas rendre
     * illisibles les enregistrements qualité produits pendant le contrat.
     *
     * @throws ModuleActivationFailedException le moteur a refusé ou n'a pas
     *                                         répondu.
     */
    void deactivate(UUID tenantId, String moduleCode);
}
