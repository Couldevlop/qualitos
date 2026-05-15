package com.openlab.qualitos.quality.training;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainingEnrollmentRepository extends JpaRepository<TrainingEnrollment, UUID> {

    Optional<TrainingEnrollment> findByTenantIdAndUserIdAndPathId(
            UUID tenantId, UUID userId, UUID pathId);

    Page<TrainingEnrollment> findByTenantIdAndUserId(UUID tenantId, UUID userId, Pageable pageable);

    Page<TrainingEnrollment> findByTenantIdAndPathId(UUID tenantId, UUID pathId, Pageable pageable);

    Page<TrainingEnrollment> findByTenantIdAndStatus(
            UUID tenantId, EnrollmentStatus status, Pageable pageable);

    /** Public verification — pas de filtre tenant ; le certificateCode est l'autorité. */
    Optional<TrainingEnrollment> findByCertificateCode(String certificateCode);
}
