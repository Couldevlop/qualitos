package com.openlab.qualitos.quality.derogation.domain;

/**
 * Le geste demandé ne s'applique pas à l'état du dossier.
 *
 * <p>Le message DIT l'état où l'on est et ce qu'il attend : « ce dossier est
 * déjà parti au client » se comprend, « transition invalide » non.
 */
public class DerogationStateException extends RuntimeException {

    public DerogationStateException(String message) {
        super(message);
    }
}
