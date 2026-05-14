package com.openlab.qualitos.quality.docs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    Optional<DocumentVersion> findByIdAndDocumentId(UUID id, UUID documentId);

    List<DocumentVersion> findByDocumentIdAndStatus(UUID documentId, VersionStatus status);
}
