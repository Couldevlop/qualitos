package com.openlab.qualitos.quality.academy.infrastructure;

import com.openlab.qualitos.quality.academy.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByModuleIdOrderByOrderIndexAsc(UUID moduleId);

    List<Lesson> findByModuleIdInOrderByOrderIndexAsc(List<UUID> moduleIds);

    Optional<Lesson> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByModuleIdAndOrderIndex(UUID moduleId, int orderIndex);

    long countByModuleId(UUID moduleId);
}
