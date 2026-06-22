package com.openlab.qualitos.quality.academy.infrastructure;

import com.openlab.qualitos.quality.academy.domain.AcademyCourse;
import com.openlab.qualitos.quality.academy.domain.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AcademyCourseRepository extends JpaRepository<AcademyCourse, UUID> {

    Optional<AcademyCourse> findByTenantIdAndCode(UUID tenantId, String code);

    Page<AcademyCourse> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AcademyCourse> findByTenantIdAndStatus(UUID tenantId, CourseStatus status, Pageable pageable);

    Page<AcademyCourse> findByTenantIdAndTargetRole(UUID tenantId, String targetRole, Pageable pageable);

    Page<AcademyCourse> findByTenantIdAndIndustrySector(UUID tenantId, String industrySector, Pageable pageable);
}
