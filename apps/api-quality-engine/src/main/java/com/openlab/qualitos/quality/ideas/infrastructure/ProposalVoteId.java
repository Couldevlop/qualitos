package com.openlab.qualitos.quality.ideas.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ProposalVoteId implements Serializable {

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "voter_id", nullable = false)
    private UUID voterId;

    protected ProposalVoteId() {}

    public ProposalVoteId(UUID proposalId, UUID voterId) {
        this.proposalId = proposalId;
        this.voterId = voterId;
    }

    public UUID getProposalId() { return proposalId; }
    public UUID getVoterId() { return voterId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProposalVoteId other)) return false;
        return Objects.equals(proposalId, other.proposalId)
                && Objects.equals(voterId, other.voterId);
    }

    @Override
    public int hashCode() { return Objects.hash(proposalId, voterId); }
}
