package com.openlab.qualitos.quality.ideas.domain;

/** Une transition impossible depuis l'état courant, ou une donnée manquante. */
public class IdeaStateException extends RuntimeException {
    public IdeaStateException(String message) { super(message); }
}
