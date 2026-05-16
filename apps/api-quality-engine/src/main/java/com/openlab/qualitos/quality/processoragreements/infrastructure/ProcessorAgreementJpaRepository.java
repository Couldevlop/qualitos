package com.openlab.qualitos.quality.processoragreements.infrastructure;

import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessorAgreementJpaRepository
        extends JpaRepository<ProcessorAgreementJpaEntity, UUID> {

    Optional<ProcessorAgreementJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<ProcessorAgreementJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<ProcessorAgreementJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, ProcessorAgreementStatus status, Pageable pageable);

    Optional<ProcessorAgreementJpaEntity> findByTenantIdAndReference(
            UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    @Query("select e from ProcessorAgreementJpaEntity e " +
           "where e.status = :active " +
           "  and e.expirationDate is not null and e.expirationDate <= :now " +
           "order by e.expirationDate asc")
    List<ProcessorAgreementJpaEntity> findExpirable(
            @Param("active") ProcessorAgreementStatus active,
            @Param("now") Instant now,
            Pageable pageable);
}
