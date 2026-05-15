package com.openlab.qualitos.quality.calibration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CalibrationRecordRepository extends JpaRepository<CalibrationRecord, UUID> {

    Page<CalibrationRecord> findByEquipmentIdOrderByPerformedOnDesc(
            UUID equipmentId, Pageable pageable);

    @Query("select max(r.performedOn) from CalibrationRecord r where r.equipmentId = :equipmentId")
    Optional<LocalDate> findLastPerformedOn(UUID equipmentId);

    long countByEquipmentIdAndResult(UUID equipmentId, CalibrationResult result);

    void deleteByEquipmentId(UUID equipmentId);
}
