package com.openlab.qualitos.quality.apikeys.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyJpaRepository extends JpaRepository<ApiKeyJpaEntity, UUID> {

    Optional<ApiKeyJpaEntity> findByPrefix(String prefix);

    List<ApiKeyJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Query("""
            select k from ApiKeyJpaEntity k
            where k.status = 'ACTIVE'
              and k.expiresAt is not null
              and k.expiresAt <= :now
            order by k.expiresAt asc
            """)
    List<ApiKeyJpaEntity> findExpirable(@Param("now") Instant now, Pageable pageable);
}
