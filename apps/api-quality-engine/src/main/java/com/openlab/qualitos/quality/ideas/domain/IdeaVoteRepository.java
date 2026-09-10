package com.openlab.qualitos.quality.ideas.domain;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IdeaVoteRepository {

    /** Pose une voix. Sans effet si elle existe déjà : voter deux fois n'ajoute rien. */
    void add(IdeaVote vote);

    /** @return vrai si une voix a bien été retirée. */
    boolean remove(UUID ideaId, UUID voterId);

    /** Nombre de voix par idée, pour tout le client — le tableau se lit d'un coup. */
    Map<UUID, Long> countByIdea(UUID tenantId);

    /** Les idées que cette personne soutient. */
    Set<UUID> ideasVotedBy(UUID tenantId, UUID voterId);
}
