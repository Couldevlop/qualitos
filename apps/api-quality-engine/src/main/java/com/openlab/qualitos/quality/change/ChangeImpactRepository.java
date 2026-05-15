package com.openlab.qualitos.quality.change;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChangeImpactRepository extends JpaRepository<ChangeImpact, UUID> {

    List<ChangeImpact> findByChangeId(UUID changeId);

    Optional<ChangeImpact> findByChangeIdAndTargetTypeAndTargetId(
            UUID changeId, ChangeImpactTargetType type, UUID targetId);

    void deleteByChangeId(UUID changeId);
}
