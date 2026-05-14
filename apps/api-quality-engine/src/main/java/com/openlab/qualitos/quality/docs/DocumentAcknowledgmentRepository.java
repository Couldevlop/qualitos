package com.openlab.qualitos.quality.docs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentAcknowledgmentRepository extends JpaRepository<DocumentAcknowledgment, UUID> {

    Optional<DocumentAcknowledgment> findByVersionIdAndUserId(UUID versionId, UUID userId);

    long countByVersionId(UUID versionId);
}
