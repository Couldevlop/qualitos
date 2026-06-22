package com.openlab.qualitos.quality.academy.infrastructure;

import com.openlab.qualitos.quality.academy.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    Optional<Quiz> findByModuleId(UUID moduleId);

    Optional<Quiz> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByModuleId(UUID moduleId);
}
