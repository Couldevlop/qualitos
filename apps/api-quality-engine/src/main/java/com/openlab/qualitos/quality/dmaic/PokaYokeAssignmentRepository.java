package com.openlab.qualitos.quality.dmaic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PokaYokeAssignmentRepository extends JpaRepository<PokaYokeAssignment, UUID> {

    Optional<PokaYokeAssignment> findByIdAndProjectId(UUID id, UUID projectId);

    List<PokaYokeAssignment> findByProjectId(UUID projectId);
}
