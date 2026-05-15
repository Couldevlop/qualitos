package com.openlab.qualitos.quality.dmaic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessMeasureRepository extends JpaRepository<ProcessMeasure, UUID> {

    Optional<ProcessMeasure> findByIdAndProjectId(UUID id, UUID projectId);

    List<ProcessMeasure> findByProjectIdOrderByRecordedAtAsc(UUID projectId);
}
