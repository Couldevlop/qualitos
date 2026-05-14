package com.openlab.qualitos.quality.pdca;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PdcaStepRepository extends JpaRepository<PdcaStep, UUID> {

    Optional<PdcaStep> findByIdAndCycleId(UUID id, UUID cycleId);
}
