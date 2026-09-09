package com.openlab.qualitos.quality.ideas.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProposalVoteJpaRepository
        extends JpaRepository<ProposalVoteJpaEntity, ProposalVoteId> {

    /** Le décompte pour tout le client, en une requête : le tableau se lit d'un coup. */
    @Query("""
           SELECT v.id.proposalId, COUNT(v)
             FROM ProposalVoteJpaEntity v
            WHERE v.tenantId = :tenantId
            GROUP BY v.id.proposalId
           """)
    List<Object[]> countByIdea(@Param("tenantId") UUID tenantId);

    @Query("""
           SELECT v.id.proposalId
             FROM ProposalVoteJpaEntity v
            WHERE v.tenantId = :tenantId AND v.id.voterId = :voterId
           """)
    List<UUID> ideasVotedBy(@Param("tenantId") UUID tenantId, @Param("voterId") UUID voterId);
}
