package com.openlab.qualitos.quality.consent.infrastructure;

import com.openlab.qualitos.quality.consent.domain.ConsentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentJpaRepository extends JpaRepository<ConsentJpaEntity, UUID> {

    Optional<ConsentJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<ConsentJpaEntity> findByTenantIdAndSubjectIdentifierHash(
            UUID tenantId, String subjectIdentifierHash, Pageable pageable);

    Page<ConsentJpaEntity> findByTenantIdAndPurposeCode(
            UUID tenantId, String purposeCode, Pageable pageable);

    @Query("select e from ConsentJpaEntity e " +
           "where e.tenantId = :tenantId " +
           "  and e.subjectIdentifierHash = :hash " +
           "  and e.purposeCode = :purpose " +
           "  and e.status = :granted " +
           "  and (e.expiresAt is null or e.expiresAt > :now) " +
           "order by e.grantedAt desc")
    List<ConsentJpaEntity> findActiveByPurpose(
            @Param("tenantId") UUID tenantId,
            @Param("hash") String hash,
            @Param("purpose") String purpose,
            @Param("granted") ConsentStatus granted,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("select e from ConsentJpaEntity e " +
           "where e.status = :granted " +
           "  and e.expiresAt is not null and e.expiresAt <= :now " +
           "order by e.expiresAt asc")
    List<ConsentJpaEntity> findExpirable(
            @Param("granted") ConsentStatus granted,
            @Param("now") Instant now,
            Pageable pageable);
}
