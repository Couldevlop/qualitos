package com.openlab.qualitos.quality.circle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CircleMeetingRepository extends JpaRepository<CircleMeeting, UUID> {

    Optional<CircleMeeting> findByIdAndCircleId(UUID id, UUID circleId);
}
