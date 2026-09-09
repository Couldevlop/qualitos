package com.openlab.qualitos.quality.ideas.domain;

import java.time.Instant;
import java.util.UUID;

/** Une voix : qui soutient quelle idée, et depuis quand. */
public class IdeaVote {

    private final UUID ideaId;
    private final UUID voterId;
    private final UUID tenantId;
    private final Instant castAt;

    private IdeaVote(UUID ideaId, UUID voterId, UUID tenantId, Instant castAt) {
        this.ideaId = ideaId;
        this.voterId = voterId;
        this.tenantId = tenantId;
        this.castAt = castAt;
    }

    public static IdeaVote cast(UUID ideaId, UUID voterId, UUID tenantId, Instant now) {
        if (ideaId == null || voterId == null || tenantId == null) {
            throw new IdeaStateException("A vote names an idea, a voter and a tenant");
        }
        return new IdeaVote(ideaId, voterId, tenantId, now);
    }

    public UUID getIdeaId() { return ideaId; }
    public UUID getVoterId() { return voterId; }
    public UUID getTenantId() { return tenantId; }
    public Instant getCastAt() { return castAt; }
}
