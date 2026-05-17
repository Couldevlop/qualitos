package com.openlab.qualitos.quality.ims.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NormClauseMappingJpaRepository extends JpaRepository<NormClauseMappingEntity, UUID> {

    @Query("""
        SELECT m FROM NormClauseMappingEntity m
        WHERE m.sourceStandardCode IN :codes
          AND m.targetStandardCode IN :codes
        """)
    List<NormClauseMappingEntity> findBetween(@Param("codes") List<String> codes);
}
