package com.openlab.qualitos.quality.ideas.domain;

/**
 * On ne vote plus sur une idée tranchée.
 *
 * <p>Distincte de {@link IdeaStateException} parce qu'elle rend 409 et non 422 :
 * la demande est licite, c'est le moment qui ne l'est plus.
 */
public class VoteClosedException extends RuntimeException {
    public VoteClosedException(java.util.UUID id) {
        super("Voting is closed on idea " + id + ": it has already been decided");
    }
}
