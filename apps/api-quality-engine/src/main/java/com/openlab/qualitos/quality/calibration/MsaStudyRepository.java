package com.openlab.qualitos.quality.calibration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MsaStudyRepository extends JpaRepository<MsaStudy, UUID> {

    Page<MsaStudy> findByEquipmentIdOrderByPerformedOnDesc(UUID equipmentId, Pageable pageable);

    long countByEquipmentIdAndResult(UUID equipmentId, MsaResult result);

    void deleteByEquipmentId(UUID equipmentId);
}
