package com.openlab.qualitos.quality.academy.infrastructure;

import com.openlab.qualitos.quality.academy.domain.AcademyModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademyModuleRepository extends JpaRepository<AcademyModule, UUID> {

    List<AcademyModule> findByCourseIdOrderByOrderIndexAsc(UUID courseId);

    Optional<AcademyModule> findByTenantIdAndId(UUID tenantId, UUID id);

    long countByCourseId(UUID courseId);

    boolean existsByCourseIdAndOrderIndex(UUID courseId, int orderIndex);
}
