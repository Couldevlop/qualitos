package com.openlab.qualitos.quality.calibration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CalibrationEquipmentRepository extends JpaRepository<CalibrationEquipment, UUID> {

    Optional<CalibrationEquipment> findByTenantIdAndCode(UUID tenantId, String code);

    Page<CalibrationEquipment> findByTenantId(UUID tenantId, Pageable pageable);

    Page<CalibrationEquipment> findByTenantIdAndStatus(UUID tenantId, EquipmentStatus status, Pageable pageable);

    long countByTenantIdAndCritical(UUID tenantId, boolean critical);
}
