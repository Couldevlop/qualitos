package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MarketplacePackJpaRepository extends JpaRepository<MarketplacePackJpaEntity, UUID> {

    boolean existsByPackIdAndVersion(String packId, String version);

    @Query("""
        SELECT p FROM MarketplacePackJpaEntity p
        WHERE p.status = com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStatus.PUBLISHED
          AND (:sector IS NULL OR p.sector = :sector)
        ORDER BY p.ratingAvg DESC, p.updatedAt DESC
        """)
    List<MarketplacePackJpaEntity> findPublished(@Param("sector") String sector);

    List<MarketplacePackJpaEntity> findByStatusOrderBySubmittedAtAsc(MarketplacePackStatus status);

    @Query("""
        SELECT p FROM MarketplacePackJpaEntity p
        WHERE p.status IN (
            com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStatus.SUBMITTED,
            com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStatus.IN_REVIEW)
        ORDER BY p.submittedAt ASC
        """)
    List<MarketplacePackJpaEntity> findModerationQueue();
}
