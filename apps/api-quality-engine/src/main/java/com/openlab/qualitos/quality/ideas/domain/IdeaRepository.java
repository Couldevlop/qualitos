package com.openlab.qualitos.quality.ideas.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdeaRepository {

    Idea save(Idea idea);

    Optional<Idea> findByIdAndTenant(UUID id, UUID tenantId);

    /** Toutes les idées du client, les plus récentes d'abord. */
    List<Idea> findByTenant(UUID tenantId);
}
