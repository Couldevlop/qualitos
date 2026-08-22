package com.openlab.qualitos.quality.common;

/**
 * 403. L'utilisateur est bien authentifié et bien habilité — il lui manque le
 * second facteur que cette action exige (CLAUDE.md §18.2 règle 5).
 *
 * <p>Pas un 401 : renvoyer 401 dirait « votre session est invalide » et
 * déclencherait une reconnexion silencieuse qui reproduirait exactement le même
 * jeton. Le 403 dit ce qui manque, et le message dit quoi faire.
 */
public class StepUpRequiredException extends RuntimeException {

    private final String action;

    public StepUpRequiredException(String action) {
        super("Cette action exige une authentification à deux facteurs : " + action
                + ". Reconnectez-vous avec votre code à usage unique.");
        this.action = action;
    }

    /** L'action refusée, exposée au client pour qu'il sache quoi reprendre après la reconnexion. */
    public String getAction() {
        return action;
    }
}
