package com.openlab.qualitos.quality.change;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChangeApprovalRepository extends JpaRepository<ChangeApproval, UUID> {

    List<ChangeApproval> findByChangeIdOrderByApprovalLevelAsc(UUID changeId);

    Optional<ChangeApproval> findByChangeIdAndApproverUserId(UUID changeId, UUID approverUserId);

    long countByChangeIdAndDecision(UUID changeId, ApprovalDecision decision);

    long countByChangeId(UUID changeId);

    void deleteByChangeId(UUID changeId);
}
