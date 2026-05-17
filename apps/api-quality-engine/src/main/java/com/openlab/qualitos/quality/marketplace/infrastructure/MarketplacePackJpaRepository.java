package com.openlab.qualitos.quality.marketplace.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MarketplacePackJpaRepository extends JpaRepository<MarketplacePackJpaEntity, UUID> {

    boolean existsByPackIdAndVersion(String packId, String version);

    @Query("""
        SELECT p FROM MarketplacePackJpaEntity p
        WHERE p.verified = true
          AND (:sector IS NULL OR p.sector = :sector)
        ORDER BY p.updatedAt DESC
        """)
    List<MarketplacePackJpaEntity> findVerified(@Param("sector") String sector);
}
