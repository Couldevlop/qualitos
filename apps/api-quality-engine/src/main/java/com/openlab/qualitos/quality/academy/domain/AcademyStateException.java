package com.openlab.qualitos.quality.academy.domain;

/**
 * Transition ou opération invalide dans le cycle de vie d'une ressource
 * d'académie (ex. inscrire à un cours non publié, compléter une inscription
 * annulée, publier un cours sans module). Mappée en 409.
 */
public class AcademyStateException extends RuntimeException {

    public AcademyStateException(String message) {
        super(message);
    }
}
