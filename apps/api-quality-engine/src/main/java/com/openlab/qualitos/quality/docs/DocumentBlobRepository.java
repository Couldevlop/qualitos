package com.openlab.qualitos.quality.docs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentBlobRepository extends JpaRepository<DocumentBlob, UUID> {

    Optional<DocumentBlob> findByTenantIdAndSha256(UUID tenantId, String sha256);

    boolean existsByObjectKey(String objectKey);

    @Query("select count(b) from DocumentBlob b where b.tenantId = :tenantId")
    long countFiles(@Param("tenantId") UUID tenantId);

    /** Ce que les fichiers du client pèseraient sans compression ni dédoublonnage. */
    @Query("select coalesce(sum(b.originalSize), 0) from DocumentBlob b where b.tenantId = :tenantId")
    long sumOriginalBytes(@Param("tenantId") UUID tenantId);

    /** Ce que le client occupe réellement dans le stockage. */
    @Query("select coalesce(sum(b.storedSize), 0) from DocumentBlob b where b.tenantId = :tenantId")
    long sumStoredBytes(@Param("tenantId") UUID tenantId);
}
