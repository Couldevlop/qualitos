package com.openlab.qualitos.quality.ideas.domain;

/** L'idée n'existe pas — ou pas dans ce client, ce qui revient au même ici. */
public class IdeaNotFoundException extends RuntimeException {
    public IdeaNotFoundException(java.util.UUID id) { super("Idea not found: " + id); }
}
