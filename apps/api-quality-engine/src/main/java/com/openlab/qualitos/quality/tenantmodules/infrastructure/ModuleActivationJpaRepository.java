package com.openlab.qualitos.quality.tenantmodules.infrastructure;

import com.openlab.qualitos.quality.tenantmodules.domain.ActivationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleActivationJpaRepository extends JpaRepository<ModuleActivationJpaEntity, UUID> {

    /**
     * Activation non-terminale (TRIAL/ACTIVE/SUSPENDED). Au plus une par (tenant, code)
     * en pratique — la garantie d'unicité est applicative (vs partial unique index DB
     * non disponible sur tous les vendeurs).
     */
    @Query("""
            select a from ModuleActivationJpaEntity a
            where a.tenantId = :tenantId
              and a.moduleCode = :code
              and a.status not in ('EXPIRED','DISABLED')
            """)
    Optional<ModuleActivationJpaEntity> findOpen(@Param("tenantId") UUID tenantId,
                                                  @Param("code") String code);

    List<ModuleActivationJpaEntity> findByTenantIdOrderByActivatedAtDesc(UUID tenantId);

    @Query("""
            select a from ModuleActivationJpaEntity a
            where a.tenantId = :tenantId
              and a.status in ('TRIAL','ACTIVE')
            """)
    List<ModuleActivationJpaEntity> findEnabled(@Param("tenantId") UUID tenantId);

    @Query("""
            select a from ModuleActivationJpaEntity a
            where a.status not in ('EXPIRED','DISABLED')
              and (
                   (a.trialEndsAt is not null and a.trialEndsAt <= :now)
                or (a.expiresAt   is not null and a.expiresAt   <= :now)
              )
            order by a.trialEndsAt asc nulls last, a.expiresAt asc nulls last
            """)
    List<ModuleActivationJpaEntity> findDueForExpiration(@Param("now") Instant now, Pageable pageable);
}
