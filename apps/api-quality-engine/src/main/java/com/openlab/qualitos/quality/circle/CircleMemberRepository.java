package com.openlab.qualitos.quality.circle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CircleMemberRepository extends JpaRepository<CircleMember, UUID> {

    Optional<CircleMember> findByIdAndCircleId(UUID id, UUID circleId);

    boolean existsByCircleIdAndUserId(UUID circleId, UUID userId);
}
