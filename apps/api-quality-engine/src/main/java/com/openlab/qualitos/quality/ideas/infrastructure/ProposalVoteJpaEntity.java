package com.openlab.qualitos.quality.ideas.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Une voix. La clé composite porte la règle « une par personne et par idée ». */
@Entity
@Table(name = "proposal_votes")
public class ProposalVoteJpaEntity {

    @EmbeddedId
    private ProposalVoteId id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProposalVoteJpaEntity() {}

    public ProposalVoteJpaEntity(ProposalVoteId id, UUID tenantId, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.createdAt = createdAt;
    }

    public ProposalVoteId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public Instant getCreatedAt() { return createdAt; }
}
