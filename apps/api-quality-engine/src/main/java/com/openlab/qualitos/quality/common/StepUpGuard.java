package com.openlab.qualitos.quality.common;

import org.springframework.stereotype.Component;

/**
 * Porte d'entrée des actions critiques : signature d'un document opposable,
 * acceptation d'une révision qui en modifie un.
 *
 * <p>Un appel explicite plutôt qu'un aspect ou une annotation maison : on lit,
 * dans le corps de la méthode, que l'action exige un second facteur. Une
 * annotation qu'un intercepteur mal configuré n'appliquerait plus laisserait la
 * même méthode s'exécuter sans bruit.
 */
@Component
public class StepUpGuard {

    private final StepUpProperties properties;

    public StepUpGuard(StepUpProperties properties) {
        this.properties = properties;
    }

    /**
     * @param action ce que l'utilisateur tentait de faire, repris dans le message
     *               d'erreur — « approuver un control plan » se comprend, un code
     *               d'erreur nu, non.
     * @throws StepUpRequiredException si le jeton ne porte aucune preuve de second facteur.
     */
    public void require(String action) {
        if (!StepUpAuthentication.satisfied(properties)) {
            throw new StepUpRequiredException(action);
        }
    }
}
