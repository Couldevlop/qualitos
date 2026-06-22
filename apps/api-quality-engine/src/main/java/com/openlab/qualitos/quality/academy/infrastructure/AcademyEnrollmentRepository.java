package com.openlab.qualitos.quality.academy.infrastructure;

import com.openlab.qualitos.quality.academy.domain.AcademyEnrollment;
import com.openlab.qualitos.quality.academy.domain.AcademyEnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AcademyEnrollmentRepository extends JpaRepository<AcademyEnrollment, UUID> {

    Optional<AcademyEnrollment> findByTenantIdAndUserIdAndCourseId(UUID tenantId, UUID userId, UUID courseId);

    Optional<AcademyEnrollment> findByTenantIdAndId(UUID tenantId, UUID id);

    Page<AcademyEnrollment> findByTenantIdAndUserId(UUID tenantId, UUID userId, Pageable pageable);

    Page<AcademyEnrollment> findByTenantIdAndCourseId(UUID tenantId, UUID courseId, Pageable pageable);

    Page<AcademyEnrollment> findByTenantIdAndStatus(UUID tenantId, AcademyEnrollmentStatus status, Pageable pageable);
}
