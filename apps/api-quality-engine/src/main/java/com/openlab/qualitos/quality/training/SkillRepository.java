package com.openlab.qualitos.quality.training;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    Optional<Skill> findByTenantIdAndCode(UUID tenantId, String code);

    Page<Skill> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Skill> findByTenantIdAndCategory(UUID tenantId, String category, Pageable pageable);
}
