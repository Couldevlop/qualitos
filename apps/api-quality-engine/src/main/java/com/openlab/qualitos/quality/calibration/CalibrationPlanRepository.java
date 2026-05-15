package com.openlab.qualitos.quality.calibration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CalibrationPlanRepository extends JpaRepository<CalibrationPlan, UUID> {

    Optional<CalibrationPlan> findByEquipmentId(UUID equipmentId);

    Page<CalibrationPlan> findByTenantIdAndNextDueOnBefore(
            UUID tenantId, LocalDate cutoff, Pageable pageable);

    void deleteByEquipmentId(UUID equipmentId);
}
