package com.openlab.qualitos.quality.docs;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    Optional<DocumentVersion> findByIdAndDocumentId(UUID id, UUID documentId);

    List<DocumentVersion> findByDocumentIdAndStatus(UUID documentId, VersionStatus status);

    boolean existsByBlobId(UUID blobId);

    /** Ce que pèseraient les fichiers de toutes les versions d'un client sans dédoublonnage. */
    @Query("select coalesce(sum(b.originalSize), 0) from DocumentVersion v join v.blob b "
            + "where v.document.tenantId = :tenantId")
    long sumLogicalBytes(@Param("tenantId") UUID tenantId);

    /**
     * Les versions obsolètes d'un client dont le fichier a dépassé sa durée de
     * conservation, par lots : le balayage reprend au passage suivant.
     */
    @Query("select v from DocumentVersion v join fetch v.blob b "
            + "where v.document.tenantId = :tenantId and v.status = :status "
            + "and v.obsoletedAt < :before order by v.obsoletedAt asc")
    List<DocumentVersion> findPurgeable(@Param("tenantId") UUID tenantId,
                                        @Param("status") VersionStatus status,
                                        @Param("before") Instant before,
                                        Pageable pageable);
}
